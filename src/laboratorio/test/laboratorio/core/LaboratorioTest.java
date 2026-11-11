package laboratorio.core;

import laboratorio.modello.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe di test per {@link Laboratorio}.
 * 
 * Verifica la logica del sistema, coprendo la gestione del catalogo, 
 * i flussi di prestito/restituzione e le dinamiche concorrenziali. 
 * I test si assicurano che il laboratorio assegni correttamente gli strumenti, 
 * metta in attesa chi richiede risorse occupate e gestisca in sicurezza l'accesso 
 * simultaneo da parte di più thread senza creare deadlock.
 */
class LaboratorioTest {

    private static final TipoStrumento OSC = TipoStrumento.OSCILLOSCOPIO;

    private Laboratorio lab;

    // usati dai thread dei test di attesa
    private String codiceKimi;
    private boolean kimiInterrotto;

    /**
     * Inizializza un nuovo Laboratorio pulito prima di ogni test,
     * per evitare che lo stato di un test influenzi i successivi.
     */
    @BeforeEach
    void setUp() {
        lab = new Laboratorio();
    }

    // ---------- catalogo ----------

    /**
     * Verifica che non sia possibile aggirare il limite fisico di capienza
     * del laboratorio aggiungendo più strumenti del consentito.
     */
    @Test
    void nonSiPossonoAggiungerePiuStrumentiDelMassimo() {
        for (int i = 1; i <= Laboratorio.MAX_STRUMENTI; i++) {
            lab.aggiungiStrumento(new Strumento("S-" + i, TipoStrumento.GENERATORE));
        }
        assertThrows(IllegalStateException.class,
                () -> lab.aggiungiStrumento(new Strumento("S-5", TipoStrumento.GENERATORE)));
    }

    /**
     * Assicura l'univocità dei codici identificativi all'interno del catalogo.
     */
    @Test
    void nonSiPossonoAggiungereDueStrumentiConLoStessoCodice() {
        lab.aggiungiStrumento(new Strumento("OSC-01", OSC));
        assertThrows(IllegalArgumentException.class,
                () -> lab.aggiungiStrumento(new Strumento("OSC-01", OSC)));
    }

    /**
     * Protegge i dati in uso: blocca la rimozione dal catalogo di uno strumento
     * che al momento si trova nelle mani di un utente.
     */
    @Test
    void nonSiPuoRimuovereUnoStrumentoInPrestito() throws Exception {
        lab.aggiungiStrumento(new Strumento("OSC-01", OSC));
        lab.richiedi("Alysa", OSC);

        assertThrows(IllegalStateException.class, () -> lab.rimuoviStrumento("OSC-01"));
    }

    // ---------- richiesta e restituzione ----------

    /**
     * Controlla che richiedendo uno strumento
     * disponibile questo venga correttamente marcato come IN_PRESTITO all'utente.
     */
    @Test
    void unoStrumentoDisponibileVieneAssegnato() throws Exception {
        lab.aggiungiStrumento(new Strumento("OSC-01", OSC));

        String codice = lab.richiedi("Alysa", OSC);

        assertEquals("OSC-01", codice);
        StrumentoView s = lab.snapshot().strumenti().get(0);
        assertEquals(StatoStrumento.IN_PRESTITO, s.stato());
        assertEquals("Alysa", s.assegnatoA());
    }

    /**
     * Verifica che il sistema prevenga richieste per tipologie di strumenti 
     * non esistenti o non censite nel catalogo.
     */
    @Test
    void richiedereUnTipoNonPresenteDaErrore() {
        lab.aggiungiStrumento(new Strumento("OSC-01", OSC));

        assertThrows(IllegalArgumentException.class,
                () -> lab.richiedi("Alysa", TipoStrumento.GENERATORE));
    }

    /**
     * Verifica che il rientro di uno strumento ne pulisca lo stato, 
     * rendendolo nuovamente disponibile per tutti.
     */
    @Test
    void restituireLoStrumentoLoRendeDisponibile() throws Exception {
        lab.aggiungiStrumento(new Strumento("OSC-01", OSC));
        lab.richiedi("Alysa", OSC);

        lab.restituisci("Alysa", "OSC-01");

        StrumentoView s = lab.snapshot().strumenti().get(0);
        assertEquals(StatoStrumento.DISPONIBILE, s.stato());
        assertNull(s.assegnatoA());
    }

    /**
     * Controllo di sicurezza: un utente non può sbloccare la restituzione
     * di uno strumento che è stato assegnato a un'altra persona.
     */
    @Test
    void restituireLoStrumentoDaUnAltroUtenteDaErrore() throws Exception {
        lab.aggiungiStrumento(new Strumento("OSC-01", OSC));
        lab.richiedi("Alysa", OSC);

        assertThrows(IllegalStateException.class, () -> lab.restituisci("Kimi", "OSC-01"));
        assertEquals("Alysa", lab.snapshot().strumenti().get(0).assegnatoA());
    }

    // ---------- attesa e interruzione (con thread) ----------

    /**
     * Testa il meccanismo di accodamento (wait/notify). Se uno strumento è occupato, 
     * il thread del secondo utente deve rimanere bloccato (in attesa) finché 
     * il primo utente non lo restituisce.
     */
    @Test
    void unaRichiestaAspettaFinoAllaRestituzione() throws Exception {
        lab.aggiungiStrumento(new Strumento("OSC-01", OSC));
        lab.richiedi("Alysa", OSC); // l'unico oscilloscopio è occupato

        Thread kimi = new Thread(() -> {
            try {
                codiceKimi = lab.richiedi("Kimi", OSC);
            } catch (InterruptedException e) {
                kimiInterrotto = true;
            }
        });
        kimi.start();
        Thread.sleep(200); // lascia tempo a Kimi di mettersi in attesa

        assertTrue(kimi.isAlive(), "Kimi non deve ottenere lo strumento mentre è occupato");

        lab.restituisci("Alysa", "OSC-01");
        kimi.join(5000);

        assertEquals("OSC-01", codiceKimi);
    }

    /**
     * Verifica la corretta gestione dell'annullamento di una richiesta in sospeso 
     * a seguito dell'interruzione del thread. 
     * Il processo deve interrompersi in modo sicuro, registrando l'annullamento 
     * nello storico e garantendo che lo strumento rimanga assegnato a chi lo sta 
     * attualmente utilizzando.
     * 
     */
    @Test
    void unaRichiestaInterrottaMentreAspettaVieneAnnullata() throws Exception {
        lab.aggiungiStrumento(new Strumento("OSC-01", OSC));
        lab.richiedi("Alysa", OSC);

        Thread kimi = new Thread(() -> {
            try {
                codiceKimi = lab.richiedi("Kimi", OSC);
            } catch (InterruptedException e) {
                kimiInterrotto = true;
            }
        });
        kimi.start();
        Thread.sleep(200);

        kimi.interrupt();
        kimi.join(5000);

        assertTrue(kimiInterrotto);
        assertNull(codiceKimi);
        // lo strumento resta ad Alysa, Kimi non lo ha ottenuto
        assertEquals("Alysa", lab.snapshot().strumenti().get(0).assegnatoA());
        assertTrue(lab.getRegistro().getStorico().stream()
                .anyMatch(e -> e.tipo() == TipoEvento.ANNULLAMENTO));
    }

    // ---------- più utenti ----------

    /**
     * Lancia contemporaneamente 5 utenti 
     * che si contendono continuamente in loop solo 2 strumenti.
     * Verifica che non ci siano stalli (deadlocks), che nessuno rimanga 
     * appeso indefinitamente e che, alla fine, lo stato del laboratorio 
     * torni perfettamente a zero (nessun in prestito, nessuno in attesa).
     */
    @Test
    void cinqueUtentiUsanoDueStrumentiSenzaBloccarsi() throws Exception {
        lab.aggiungiStrumento(new Strumento("MUL-01", TipoStrumento.MULTIMETRO));
        lab.aggiungiStrumento(new Strumento("MUL-02", TipoStrumento.MULTIMETRO));

        List<Thread> utenti = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String nome = "Utente" + i;
            Thread t = new Thread(() -> {
                try {
                    for (int giro = 0; giro < 10; giro++) {
                        String codice = lab.richiedi(nome, TipoStrumento.MULTIMETRO);
                        lab.restituisci(nome, codice);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            utenti.add(t);
            t.start();
        }
        for (Thread t : utenti) {
            t.join(10_000);
            assertFalse(t.isAlive(), "un utente è rimasto bloccato");
        }

        StatoLab stato = lab.snapshot();
        assertEquals(2, stato.disponibili());
        assertEquals(0, stato.inPrestito());
        assertEquals(0, stato.inAttesa());
    }
}
