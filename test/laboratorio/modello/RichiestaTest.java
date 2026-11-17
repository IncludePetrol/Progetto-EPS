package laboratorio.modello;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe di test per {@link Richiesta}.
 *
 * Verifica le transizioni di stato di una richiesta di prestito
 * (in attesa, in prestito, restituita o annullata) e la copia per la GUI.
 */
class RichiestaTest {

    /**
     * Una nuova richiesta deve essere in attesa e senza strumento assegnato.
     */
    @Test
    void unaNuovaRichiestaEInAttesa() {
        Richiesta r = new Richiesta("Alysa", TipoStrumento.OSCILLOSCOPIO);

        assertEquals("Alysa", r.getUtente());
        assertEquals(TipoStrumento.OSCILLOSCOPIO, r.getTipo());
        assertEquals(StatoRichiesta.IN_ATTESA, r.getStato());
        assertNull(r.getCodiceStrumento());
    }

    /**
     * L'assegnazione deve portare la richiesta in prestito e salvare il codice dello strumento.
     */
    @Test
    void assegnareUnoStrumentoMetteLaRichiestaInPrestito() {
        Richiesta r = new Richiesta("Alysa", TipoStrumento.OSCILLOSCOPIO);

        r.assegna("OSC-01");

        assertEquals(StatoRichiesta.IN_PRESTITO, r.getStato());
        assertEquals("OSC-01", r.getCodiceStrumento());
    }

    /**
     * Dopo la restituzione la richiesta risulta chiusa, ma conserva il codice
     * dello strumento usato.
     */
    @Test
    void restituireLaRichiestaLaChiude() {
        Richiesta r = new Richiesta("Alysa", TipoStrumento.OSCILLOSCOPIO);
        r.assegna("OSC-01");

        r.restituita();

        assertEquals(StatoRichiesta.RESTITUITA, r.getStato());
        assertEquals("OSC-01", r.getCodiceStrumento());
    }

    /**
     * Una richiesta annullata non ha mai ottenuto uno strumento.
     */
    @Test
    void annullareLaRichiestaLaMetteInStatoAnnullata() {
        Richiesta r = new Richiesta("Alysa", TipoStrumento.OSCILLOSCOPIO);

        r.annulla();

        assertEquals(StatoRichiesta.ANNULLATA, r.getStato());
        assertNull(r.getCodiceStrumento());
    }

    /**
     * La vista deve riportare i dati correnti della richiesta.
     */
    @Test
    void laVistaRiportaIDatiDellaRichiesta() {
        Richiesta r = new Richiesta("Alysa", TipoStrumento.OSCILLOSCOPIO);
        r.assegna("OSC-01");

        assertEquals(new RichiestaView("Alysa", TipoStrumento.OSCILLOSCOPIO,
                StatoRichiesta.IN_PRESTITO, "OSC-01"), r.toView());
    }
}
