package laboratorio.core;

import laboratorio.modello.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

// il monitor del laboratorio
// tutti i metodi pubblici sono synchronized sullo stesso oggetto (this)
public class Laboratorio {

    public static final int MAX_STRUMENTI = 4;

    private final List<Strumento> strumenti = new ArrayList<>();
    private final List<Richiesta> richieste = new ArrayList<>();
    private final RegistroEventi registro = new RegistroEventi();

    // aggiunge uno strumento al catalogo
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

    // toglie uno strumento dal catalogo (solo se non e in prestito e non ci sono richieste in attesa per il suo tipo)
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

    // chiede uno strumento del tipo dato e se sono tutti occupati aspetta la restituzione
    // ritorna il codice dello strumento assegnato
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

    // restituisce uno strumento prestato a utente
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

    // fotografia dello stato copie immutabili, sicure da passare alla GUI
    public synchronized StatoLab snapshot() {
        List<StrumentoView> sv = strumenti.stream().map(Strumento::toView).toList();
        List<RichiestaView> rv = richieste.stream().map(Richiesta::toView).toList();
        return new StatoLab(sv, rv);
    }

    public synchronized Set<TipoStrumento> tipiPresenti() {
        Set<TipoStrumento> tipi = EnumSet.noneOf(TipoStrumento.class);
        for (Strumento s : strumenti) {
            tipi.add(s.getTipo());
        }
        return tipi;
    }

    // sostituisce il catalogo usato al caricamento da file
    public synchronized void ripristina(List<Strumento> catalogo) {
        strumenti.clear();
        strumenti.addAll(catalogo);
        notifyAll();
    }

    // il registro è thread-safe da solo, quindi lo si può prendere fuori dal lock
    public RegistroEventi getRegistro() {
        return registro;
    }

    // cerca uno strumento per codice, null se non c'è
    private Strumento trova(String codice) {
        for (Strumento s : strumenti) {
            if (s.getCodice().equals(codice)) {
                return s;
            }
        }
        return null;
    }

    // ritorna uno strumento libero del tipo richiesto SOLO se la richiesta e la prima in coda
    // (FIFO per tipo), altrimenti null
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

    private Evento evento(TipoEvento tipo, String utente, String strumento) {
        return new Evento(LocalTime.now(), tipo, utente, strumento);
    }
}
