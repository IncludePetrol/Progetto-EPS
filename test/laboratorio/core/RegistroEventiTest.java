package laboratorio.core;

import laboratorio.modello.Evento;
import laboratorio.modello.TipoEvento;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegistroEventiTest {

    private static Evento evento(String utente, TipoEvento tipo) {
        return new Evento(LocalTime.of(10, 0), tipo, utente, "OSC-01");
    }

    @Test
    void loStoricoContieneGliEventiInOrdine() {
        RegistroEventi registro = new RegistroEventi();
        Evento primo = evento("Anna", TipoEvento.ASSEGNAZIONE);
        Evento secondo = evento("Anna", TipoEvento.RESTITUZIONE);

        registro.pubblica(primo);
        registro.pubblica(secondo);

        assertEquals(List.of(primo, secondo), registro.getStorico());
    }

    @Test
    void prelevaRestituisceGliEventiNuoviSenzaCancellareLoStorico() throws Exception {
        RegistroEventi registro = new RegistroEventi();
        Evento e1 = evento("Anna", TipoEvento.ASSEGNAZIONE);
        registro.pubblica(e1);

        assertEquals(List.of(e1), registro.preleva());
        assertEquals(List.of(e1), registro.getStorico());
    }

    @Test
    void loStoricoNonSiPuoModificareDallEsterno() {
        RegistroEventi registro = new RegistroEventi();
        registro.pubblica(evento("Anna", TipoEvento.ASSEGNAZIONE));

        assertThrows(UnsupportedOperationException.class,
                () -> registro.getStorico().add(evento("Kimi", TipoEvento.ATTESA)));
    }
}
