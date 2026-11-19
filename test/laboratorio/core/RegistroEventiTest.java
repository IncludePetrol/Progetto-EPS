package laboratorio.core;

import laboratorio.modello.Evento;
import laboratorio.modello.TipoEvento;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe di test per {@link RegistroEventi}.
 * 
 * Verifica il corretto funzionamento del registro che fa da fulcro 
 * per la gestione e l'accodamento degli eventi del sistema. 
 * I test si concentrano sulla garanzia dell'ordine di inserimento, 
 * sulla lettura non distruttiva e sulla sicurezza (immutabilità) 
 * dei dati esposti all'esterno.
 */
class RegistroEventiTest {

    /**
     * Metodo di utilità per creare rapidamente istanze di mock di {@link Evento} 
     * da usare nei test, mantenendo il codice più pulito e leggibile.
     *
     * @param utente il nome dell'utente associato all'evento
     * @param tipo il tipo di evento da generare
     * @return una nuova istanza configurata di Evento
     */
    private static Evento evento(String utente, TipoEvento tipo) {
        return new Evento(LocalTime.of(10, 0), tipo, utente, "OSC-01");
    }

    /**
     * Verifica che il registro salvi gli eventi nello storico 
     * rispettando l'ordine cronologico di pubblicazione.
     */
    @Test
    void loStoricoContieneGliEventiInOrdine() {
        RegistroEventi registro = new RegistroEventi();
        Evento primo = evento("Alysa", TipoEvento.ASSEGNAZIONE);
        Evento secondo = evento("Alysa", TipoEvento.RESTITUZIONE);

        registro.pubblica(primo);
        registro.pubblica(secondo);

        assertEquals(List.of(primo, secondo), registro.getStorico());
    }

    /**
     * Assicura che il metodo preleva() restituisca gli eventi appena arrivati
     * (pronti per lo smistamento), ma senza svuotare lo storico globale, 
     * che deve rimanere intatto come log di sistema.
     * 
     * @throws Exception in caso di errori imprevisti durante il prelievo
     */
    @Test
    void prelevaRestituisceGliEventiNuoviSenzaCancellareLoStorico() throws Exception {
        RegistroEventi registro = new RegistroEventi();
        Evento e1 = evento("Alysa", TipoEvento.ASSEGNAZIONE);
        registro.pubblica(e1);

        assertEquals(List.of(e1), registro.preleva());
        assertEquals(List.of(e1), registro.getStorico());
    }

    /**
     * Verifica la sicurezza dei dati: la lista dello storico deve essere esposta 
     * in sola lettura. Se un'altra classe tenta di aggiungere un evento 
     * forzando la lista dall'esterno, il sistema deve difendersi lanciando un'eccezione.
     */
    @Test
    void loStoricoNonSiPuoModificareDallEsterno() {
        RegistroEventi registro = new RegistroEventi();
        registro.pubblica(evento("Alysa", TipoEvento.ASSEGNAZIONE));

        assertThrows(UnsupportedOperationException.class,
                () -> registro.getStorico().add(evento("Kimi", TipoEvento.ATTESA)));
    }

    /**
     * Il ripristino (es. da file) deve sostituire lo storico corrente con quello passato.
     */
    @Test
    void ripristinareSostituisceLoStorico() {
        RegistroEventi registro = new RegistroEventi();
        registro.pubblica(evento("Alysa", TipoEvento.ASSEGNAZIONE));
        Evento caricato = evento("Kimi", TipoEvento.RESTITUZIONE);

        registro.ripristina(List.of(caricato));

        assertEquals(List.of(caricato), registro.getStorico());
    }

    /**
     * Gli eventi ripristinati appartengono al passato: non devono essere notificati
     * di nuovo, quindi la coda delle notifiche va svuotata.
     *
     * @throws Exception in caso di errori imprevisti durante il prelievo
     */
    @Test
    void ripristinareSvuotaLaCodaDelleNotifiche() throws Exception {
        RegistroEventi registro = new RegistroEventi();
        registro.pubblica(evento("Alysa", TipoEvento.ASSEGNAZIONE));
        registro.ripristina(List.of(evento("Kimi", TipoEvento.ATTESA)));
        Evento nuovo = evento("Max", TipoEvento.ASSEGNAZIONE);

        registro.pubblica(nuovo);

        assertEquals(List.of(nuovo), registro.preleva());
    }
}
