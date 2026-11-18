package laboratorio.core;

import laboratorio.modello.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Gestisce come monitor le risorse e i prestiti del laboratorio.
 * Garantisce la thread-safety sincronizzando tutti i metodi pubblici sull'istanza corrente (this).
 * Implementa un sistema di code FIFO per l'assegnazione degli strumenti tramite i meccanismi di wait/notify.
 */
public class Laboratorio {
    /** Numero massimo di strumenti ospitabili contemporaneamente nel catalogo. */
    public static final int MAX_STRUMENTI = 4;

    private final List<Strumento> strumenti = new ArrayList<>();
    private final List<Richiesta> richieste = new ArrayList<>();
    private final RegistroEventi registro = new RegistroEventi();

    /**
     * Aggiunge un nuovo strumento al catalogo del laboratorio.
     * Notifica tutti i thread in attesa in caso di inserimento con successo.
     *
     * @param s lo strumento da aggiungere
     * @throws IllegalStateException se il laboratorio ha già raggiunto la capienza massima (MAX_STRUMENTI)
     * @throws IllegalArgumentException se uno strumento con lo stesso codice è già presente
     */
    public synchronized void aggiungiStrumento(Strumento s) {
        if (strumenti.size() >= MAX_STRUMENTI) {
            throw new IllegalStateException("laboratorio pieno");
        }
        if (trova(s.getCodice()) != null) {
            throw new IllegalArgumentException("codice gia presente: " + s.getCodice());
        }
        strumenti.add(s);
        notifyAll();
    }

    /**
     * Rimuove uno strumento dal catalogo specificandone il codice.
     * La rimozione è consentita solo se lo strumento non è attualmente in prestito 
     * e se non vi sono richieste in attesa per quella tipologia.
     *
     * @param codice l'identificativo univoco dello strumento da rimuovere
     * @throws IllegalArgumentException se lo strumento non è presente nel catalogo
     * @throws IllegalStateException se lo strumento è attualmente in prestito o se vi sono richieste in attesa per il suo tipo
     */
    public synchronized void rimuoviStrumento(String codice) {
        Strumento s = trova(codice);
        if (s == null) {
            throw new IllegalArgumentException("strumento non trovato: " + codice);
        }
        if (s.getStato() == StatoStrumento.IN_PRESTITO) {
            throw new IllegalStateException(codice + " e in prestito");
        }
        for (Richiesta r : richieste) {
            if (r.getTipo() == s.getTipo() && r.getStato() == StatoRichiesta.IN_ATTESA) {
                throw new IllegalStateException("ci sono richieste in attesa di " + s.getTipo());
            }
        }
        strumenti.remove(s);
    }

    /**
     * Richiede l'assegnazione di uno strumento per una specifica tipologia.
     * Se tutti gli strumenti di quel tipo sono occupati, il thread chiamante viene messo in attesa (wait).
     *
     * @param utente l'identificativo dell'utente che effettua la richiesta
     * @param tipo la tipologia di strumento richiesta
     * @return il codice univoco dello strumento assegnato
     * @throws IllegalArgumentException se la tipologia richiesta non è presente a catalogo
     * @throws InterruptedException se il thread viene interrotto durante l'attesa (la richiesta viene annullata)
     */
    public synchronized String richiedi(String utente, TipoStrumento tipo) throws InterruptedException {
        if (!tipiPresenti().contains(tipo)) {
            throw new IllegalArgumentException("nessuno strumento di tipo " + tipo);
        }

        Richiesta r = new Richiesta(utente, tipo);
        richieste.add(r);
        boolean inAttesa = false;

        try {
            Strumento libero = liberoPer(r);
            while (libero == null) {
                if (!inAttesa) {
                    registro.pubblica(evento(TipoEvento.ATTESA, utente, tipo.name()));
                    inAttesa = true;
                }
                wait();
                // il wait puo risvegliarsi "a vuoto", ricontrollo sempre la condizione
                libero = liberoPer(r);
            }

            // assegnazione e controllo sono nello stesso blocco synchronized
            libero.assegna(utente);
            r.assegna(libero.getCodice());
            registro.pubblica(evento(TipoEvento.ASSEGNAZIONE, utente, libero.getCodice()));
            return libero.getCodice();

        } catch (InterruptedException e) {
            // se interrotto mentre aspetta, la richiesta viene annullata e lo stato resta coerente
            r.annulla();
            registro.pubblica(evento(TipoEvento.ANNULLAMENTO, utente, tipo.name()));
            throw e;

        } finally {
            // la coda è cambiata o una richiesta è stata servita, sveglio gli altri
            notifyAll();
        }
    }

    /**
     * Restituisce uno strumento attualmente in prestito, rendendolo nuovamente disponibile.
     * Notifica i thread in attesa che potrebbero necessitare di questo strumento.
     *
     * @param utente l'utente che sta restituendo lo strumento
     * @param codice il codice univoco dello strumento da restituire
     * @throws IllegalArgumentException se lo strumento non esiste
     * @throws IllegalStateException se lo strumento non è in prestito o è prestato a un utente diverso
     */
    public synchronized void restituisci(String utente, String codice) {
        Strumento s = trova(codice);
        if (s == null) {
            throw new IllegalArgumentException("strumento non trovato: " + codice);
        }
        if (s.getStato() != StatoStrumento.IN_PRESTITO || !utente.equals(s.getAssegnatoA())) {
            throw new IllegalStateException(codice + " non e prestato a " + utente);
        }

        s.libera();

        for (int i = richieste.size() - 1; i >= 0; i--) {
            Richiesta r = richieste.get(i);
            if (r.getStato() == StatoRichiesta.IN_PRESTITO
                    && r.getUtente().equals(utente)
                    && codice.equals(r.getCodiceStrumento())) {
                r.restituita();
                break;
            }
        }

        registro.pubblica(evento(TipoEvento.RESTITUZIONE, utente, codice));
        notifyAll(); // chi aspettava questo tipo può ora andare avanti
    }

    /**
     * Genera una fotografia immutabile dello stato corrente del laboratorio.
     * Sicuro da passare alla GUI o ad altri thread senza rischiare data race.
     *
     * @return un oggetto StatoLab contenente le viste immutabili di strumenti e richieste
     */
    public synchronized StatoLab snapshot() {
        List<StrumentoView> sv = strumenti.stream().map(Strumento::toView).toList();
        List<RichiestaView> rv = richieste.stream().map(Richiesta::toView).toList();
        return new StatoLab(sv, rv);
    }

    /**
     * Restituisce l'insieme dei tipi di strumento attualmente registrati nel catalogo.
     *
     * @return un Set contenente le tipologie presenti
     */
    public synchronized Set<TipoStrumento> tipiPresenti() {
        Set<TipoStrumento> tipi = EnumSet.noneOf(TipoStrumento.class);
        for (Strumento s : strumenti) {
            tipi.add(s.getTipo());
        }
        return tipi;
    }

    /**
     * Sostituisce l'intero catalogo degli strumenti, ad esempio a seguito del caricamento da file.
     * Notifica tutti i thread in attesa al termine dell'operazione.
     *
     * La lista viene validata prima di modificare qualsiasi cosa, così un catalogo non valido
     * (file manomesso) non lascia il laboratorio a metà.
     *
     * @param catalogo la nuova lista di strumenti da inserire
     * @throws IllegalArgumentException se ci sono codici duplicati
     * @throws IllegalStateException se gli strumenti superano MAX_STRUMENTI o ci sono prestiti o richieste in corso
     */
    public synchronized void ripristina(List<Strumento> catalogo) {
        if (catalogo.size() > MAX_STRUMENTI) {
            throw new IllegalStateException("troppi strumenti: massimo " + MAX_STRUMENTI);
        }
        Set<String> codici = new java.util.HashSet<>();
        for (Strumento s : catalogo) {
            if (!codici.add(s.getCodice())) {
                throw new IllegalArgumentException("codice duplicato: " + s.getCodice());
            }
        }
        for (Strumento s : strumenti) {
            if (s.getStato() == StatoStrumento.IN_PRESTITO) {
                throw new IllegalStateException("ci sono prestiti in corso");
            }
        }
        for (Richiesta r : richieste) {
            if (r.getStato() == StatoRichiesta.IN_ATTESA) {
                throw new IllegalStateException("ci sono richieste in attesa");
            }
        }
        strumenti.clear();
        strumenti.addAll(catalogo);
        notifyAll();
    }

    /**
     * Restituisce l'istanza del registro eventi. 
     * Il registro è internamente thread-safe, quindi accessibile senza blocco sul monitor.
     *
     * @return il registro eventi associato al laboratorio
     */
    public RegistroEventi getRegistro() {
        return registro;
    }

    /**
     * Cerca uno strumento all'interno del catalogo in base al codice.
     *
     * @param codice l'identificativo da cercare
     * @return lo Strumento corrispondente, oppure null se non trovato
     */
    private Strumento trova(String codice) {
        for (Strumento s : strumenti) {
            if (s.getCodice().equals(codice)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Verifica la disponibilità di uno strumento per una data richiesta rispettando la politica FIFO per tipo.
     *
     * @param r la richiesta da soddisfare
     * @return lo Strumento libero, oppure null se non ce ne sono di disponibili o se c'è una richiesta meno recente per lo stesso tipo
     */
    private Strumento liberoPer(Richiesta r) {
        for (Richiesta altra : richieste) {
            if (altra == r) {
                break;
            }
            if (altra.getTipo() == r.getTipo() && altra.getStato() == StatoRichiesta.IN_ATTESA) {
                return null; // c'è una richiesta più vecchia che aspetta lo stesso tipo
            }
        }
        for (Strumento s : strumenti) {
            if (s.getTipo() == r.getTipo() && s.getStato() == StatoStrumento.DISPONIBILE) {
                return s;
            }
        }
        return null;
    }

    /**
     * Metodo di supporto per instanziare un nuovo evento con il timestamp corrente.
     *
     * @param tipo il tipo di evento
     * @param utente l'utente coinvolto nell'evento
     * @param strumento lo strumento o il tipo di strumento coinvolto
     * @return l'oggetto Evento popolato
     */
    private Evento evento(TipoEvento tipo, String utente, String strumento) {
        return new Evento(LocalTime.now(), tipo, utente, strumento);
    }
}
