package laboratorio.modello;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe di test per {@link Strumento}.
 *
 * Verifica il ciclo di vita di uno strumento (disponibile, in prestito, di nuovo disponibile)
 * e la correttezza della copia immutabile destinata alla GUI.
 */
class StrumentoTest {

    /**
     * Un nuovo strumento deve nascere disponibile e senza nessun utente associato.
     */
    @Test
    void unNuovoStrumentoEDisponibile() {
        Strumento s = new Strumento("OSC-01", TipoStrumento.OSCILLOSCOPIO);

        assertEquals("OSC-01", s.getCodice());
        assertEquals(TipoStrumento.OSCILLOSCOPIO, s.getTipo());
        assertEquals(StatoStrumento.DISPONIBILE, s.getStato());
        assertNull(s.getAssegnatoA());
    }

    /**
     * L'assegnazione deve segnare lo strumento come in prestito e ricordare l'utente.
     */
    @Test
    void assegnareLoStrumentoLoMetteInPrestito() {
        Strumento s = new Strumento("OSC-01", TipoStrumento.OSCILLOSCOPIO);

        s.assegna("Alysa");

        assertEquals(StatoStrumento.IN_PRESTITO, s.getStato());
        assertEquals("Alysa", s.getAssegnatoA());
    }

    /**
     * La restituzione deve rendere lo strumento di nuovo disponibile e cancellare l'utente.
     */
    @Test
    void liberareLoStrumentoLoRendeDisponibile() {
        Strumento s = new Strumento("OSC-01", TipoStrumento.OSCILLOSCOPIO);
        s.assegna("Alysa");

        s.libera();

        assertEquals(StatoStrumento.DISPONIBILE, s.getStato());
        assertNull(s.getAssegnatoA());
    }

    /**
     * La vista deve riportare i dati correnti e non cambiare se lo strumento
     * viene modificato dopo la sua creazione (è una fotografia).
     */
    @Test
    void laVistaEUnaFotografiaDelloStrumento() {
        Strumento s = new Strumento("OSC-01", TipoStrumento.OSCILLOSCOPIO);
        s.assegna("Alysa");

        StrumentoView vista = s.toView();
        s.libera();

        assertEquals(new StrumentoView("OSC-01", TipoStrumento.OSCILLOSCOPIO,
                StatoStrumento.IN_PRESTITO, "Alysa"), vista);
    }
}
