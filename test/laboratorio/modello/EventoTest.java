package laboratorio.modello;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe di test per {@link Evento}.
 *
 * Verifica che la descrizione leggibile sia corretta per ogni tipo di evento.
 */
class EventoTest {

    /**
     * Metodo di utilità per creare rapidamente un evento di test.
     *
     * @param tipo il tipo di evento da generare
     * @return una nuova istanza di Evento
     */
    private static Evento evento(TipoEvento tipo) {
        return new Evento(LocalTime.of(10, 0), tipo, "Alysa", "OSC-01");
    }

    /**
     * Ogni tipo di evento deve produrre la frase prevista con utente e strumento.
     */
    @Test
    void laDescrizioneDipendeDalTipoDiEvento() {
        assertEquals("Alysa ottiene OSC-01", evento(TipoEvento.ASSEGNAZIONE).descrizione());
        assertEquals("Alysa in attesa di OSC-01", evento(TipoEvento.ATTESA).descrizione());
        assertEquals("Alysa restituisce OSC-01", evento(TipoEvento.RESTITUZIONE).descrizione());
        assertEquals("Alysa annulla la richiesta di OSC-01", evento(TipoEvento.ANNULLAMENTO).descrizione());
    }
}
