package laboratorio.modello;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe di test per {@link StatoLab}.
 *
 * Verifica i conteggi di strumenti disponibili, in prestito e di richieste in attesa
 * calcolati a partire dalle viste immutabili.
 */
class StatoLabTest {

    /**
     * I conteggi devono distinguere correttamente gli stati di strumenti e richieste.
     */
    @Test
    void iContatoriRiflettonoLoStato() {
        StatoLab stato = new StatoLab(
                List.of(
                        new StrumentoView("OSC-01", TipoStrumento.OSCILLOSCOPIO, StatoStrumento.IN_PRESTITO, "Alysa"),
                        new StrumentoView("OSC-02", TipoStrumento.OSCILLOSCOPIO, StatoStrumento.DISPONIBILE, null),
                        new StrumentoView("GEN-01", TipoStrumento.GENERATORE, StatoStrumento.DISPONIBILE, null)),
                List.of(
                        new RichiestaView("Alysa", TipoStrumento.OSCILLOSCOPIO, StatoRichiesta.IN_PRESTITO, "OSC-01"),
                        new RichiestaView("Kimi", TipoStrumento.GENERATORE, StatoRichiesta.IN_ATTESA, null),
                        new RichiestaView("Alysa", TipoStrumento.GENERATORE, StatoRichiesta.ANNULLATA, null)));

        assertEquals(2, stato.disponibili());
        assertEquals(1, stato.inPrestito());
        assertEquals(1, stato.inAttesa());
    }

    /**
     * Con un laboratorio vuoto tutti i contatori devono essere a zero.
     */
    @Test
    void unStatoVuotoHaTuttiIContatoriAZero() {
        StatoLab stato = new StatoLab(List.of(), List.of());

        assertEquals(0, stato.disponibili());
        assertEquals(0, stato.inPrestito());
        assertEquals(0, stato.inAttesa());
    }
}
