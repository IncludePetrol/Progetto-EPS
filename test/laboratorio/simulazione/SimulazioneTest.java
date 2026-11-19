package laboratorio.simulazione;

import laboratorio.core.Laboratorio;
import laboratorio.modello.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe di test per {@link Simulazione}.
 *
 * Verifica la validazione dei parametri, il completamento della simulazione
 * con più utenti concorrenti (senza deadlock né strumenti rimasti in prestito)
 * e la possibilità di interromperla in sicurezza.
 */
class SimulazioneTest {

    private Laboratorio lab;

    /**
     * Prepara un laboratorio con due oscilloscopi e un generatore prima di ogni test.
     */
    @BeforeEach
    void setUp() {
        lab = new Laboratorio();
        lab.aggiungiStrumento(new Strumento("OSC-01", TipoStrumento.OSCILLOSCOPIO));
        lab.aggiungiStrumento(new Strumento("OSC-02", TipoStrumento.OSCILLOSCOPIO));
        lab.aggiungiStrumento(new Strumento("GEN-01", TipoStrumento.GENERATORE));
    }

    // ---------- parametri ----------

    /**
     * Il requisito impone almeno cinque utenti: con meno la simulazione non deve crearsi.
     */
    @Test
    void servonoAlmenoCinqueUtenti() {
        assertThrows(IllegalArgumentException.class,
                () -> new Simulazione(lab, Simulazione.MIN_UTENTI - 1, 1, 10));
    }

    /**
     * Gli utenti non possono superare i nomi disponibili, altrimenti due avrebbero lo stesso nome.
     */
    @Test
    void nonPiuUtentiDeiNomiDisponibili() {
        assertThrows(IllegalArgumentException.class,
                () -> new Simulazione(lab, Simulazione.MAX_UTENTI + 1, 1, 10));
    }

    /**
     * Ogni utente deve fare almeno un giro.
     */
    @Test
    void servePiuDiZeroGiri() {
        assertThrows(IllegalArgumentException.class,
                () -> new Simulazione(lab, Simulazione.MIN_UTENTI, 0, 10));
    }

    /**
     * La durata del prestito non può essere negativa.
     */
    @Test
    void laDurataDelPrestitoNonPuoEssereNegativa() {
        assertThrows(IllegalArgumentException.class,
                () -> new Simulazione(lab, Simulazione.MIN_UTENTI, 1, -1));
    }

    /**
     * Senza strumenti a catalogo non c'è nulla da richiedere: l'avvio deve fallire.
     */
    @Test
    void nonSiAvviaConUnLaboratorioVuoto() {
        Simulazione sim = new Simulazione(new Laboratorio(), Simulazione.MIN_UTENTI, 1, 10);

        assertThrows(IllegalStateException.class, sim::avvia);
    }

    // ---------- esecuzione ----------

    /**
     * Più utenti di quanti sono gli strumenti devono riuscire a finire tutti i loro giri,
     * lasciando alla fine tutti gli strumenti disponibili e nessuna richiesta in attesa.
     */
    @Test
    void tuttiGliUtentiFinisconoLasciandoGliStrumentiLiberi() throws Exception {
        Simulazione sim = new Simulazione(lab, 8, 3, 20);

        sim.avvia();

        assertTrue(sim.attendiFine(30_000), "la simulazione non è terminata (deadlock?)");
        assertFalse(sim.isAttiva());
        assertEquals(0, sim.threadAttivi());
        StatoLab stato = lab.snapshot();
        assertEquals(0, stato.inPrestito());
        assertEquals(0, stato.inAttesa());
        // ogni richiesta fatta è stata assegnata e restituita: 8 utenti x 3 giri
        assertEquals(24, lab.getRegistro().getStorico().stream()
                .filter(e -> e.tipo() == TipoEvento.RESTITUZIONE).count());
    }

    /**
     * Non si può avviare due volte la stessa simulazione mentre è ancora in corso.
     */
    @Test
    void nonSiPuoAvviareDueVolte() throws Exception {
        Simulazione sim = new Simulazione(lab, Simulazione.MIN_UTENTI, 1, 200);
        sim.avvia();

        assertThrows(IllegalStateException.class, sim::avvia);

        assertTrue(sim.attendiFine(30_000));
    }

    /**
     * Interrompendo la simulazione tutti i thread devono uscire e gli strumenti
     * in uso devono essere restituiti, senza lasciare il laboratorio in uno stato incoerente.
     */
    @Test
    void interrompereLaSimulazioneRestituisceGliStrumenti() throws Exception {
        Simulazione sim = new Simulazione(lab, 8, 100, 500);
        sim.avvia();
        Thread.sleep(200); // lascia tempo agli utenti di prendere gli strumenti

        sim.interrompi();

        assertTrue(sim.attendiFine(10_000), "i thread non sono usciti dopo l'interruzione");
        assertEquals(0, sim.threadAttivi());
        assertEquals(0, lab.snapshot().inPrestito());
    }

    /**
     * Se la simulazione non è mai stata avviata, attendere la fine termina subito.
     */
    @Test
    void attendereUnaSimulazioneNonAvviataTerminaSubito() throws Exception {
        Simulazione sim = new Simulazione(lab, Simulazione.MIN_UTENTI, 1, 10);

        assertTrue(sim.attendiFine(100));
        assertFalse(sim.isAttiva());
    }
}
