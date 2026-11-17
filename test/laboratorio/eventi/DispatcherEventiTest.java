package laboratorio.eventi;

import laboratorio.core.RegistroEventi;
import laboratorio.modello.Evento;
import laboratorio.modello.TipoEvento;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe di test per {@link DispatcherEventi}.
 *
 * Verifica che gli eventi pubblicati nel registro vengano inoltrati a tutti i listener
 * registrati, che un listener rimosso non riceva più nulla e che un listener
 * in errore non blocchi gli altri.
 */
class DispatcherEventiTest {

    private RegistroEventi registro;
    private DispatcherEventi dispatcher;
    private Thread thread;

    /**
     * Crea un registro e avvia il dispatcher su un thread separato prima di ogni test.
     */
    @BeforeEach
    void setUp() {
        registro = new RegistroEventi();
        dispatcher = new DispatcherEventi(registro);
        thread = new Thread(dispatcher);
        thread.start();
    }

    /**
     * Ferma il thread del dispatcher al termine di ogni test.
     */
    @AfterEach
    void tearDown() throws InterruptedException {
        thread.interrupt();
        thread.join(5000);
    }

    /**
     * Metodo di utilità per creare rapidamente un evento di test.
     *
     * @param utente il nome dell'utente associato all'evento
     * @return una nuova istanza di Evento
     */
    private static Evento evento(String utente) {
        return new Evento(LocalTime.of(10, 0), TipoEvento.ASSEGNAZIONE, utente, "OSC-01");
    }

    /**
     * Un evento pubblicato nel registro deve arrivare a tutti i listener registrati.
     */
    @Test
    void gliEventiArrivanoATuttiIListener() throws Exception {
        CountDownLatch ricevuti = new CountDownLatch(2);
        List<Evento> primo = new CopyOnWriteArrayList<>();
        List<Evento> secondo = new CopyOnWriteArrayList<>();
        dispatcher.aggiungi(e -> { primo.add(e); ricevuti.countDown(); });
        dispatcher.aggiungi(e -> { secondo.add(e); ricevuti.countDown(); });

        Evento e1 = evento("Alysa");
        registro.pubblica(e1);

        assertTrue(ricevuti.await(5, TimeUnit.SECONDS));
        assertEquals(List.of(e1), primo);
        assertEquals(List.of(e1), secondo);
    }

    /**
     * Un listener rimosso non deve più ricevere eventi, mentre gli altri continuano a riceverli.
     */
    @Test
    void unListenerRimossoNonRicevePiuEventi() throws Exception {
        CountDownLatch ricevuto = new CountDownLatch(1);
        List<Evento> rimosso = new CopyOnWriteArrayList<>();
        LabListener daRimuovere = rimosso::add;
        dispatcher.aggiungi(daRimuovere);
        dispatcher.aggiungi(e -> ricevuto.countDown());
        dispatcher.rimuovi(daRimuovere);

        registro.pubblica(evento("Alysa"));

        assertTrue(ricevuto.await(5, TimeUnit.SECONDS));
        assertTrue(rimosso.isEmpty());
    }

    /**
     * Se un listener lancia un'eccezione, gli altri devono comunque ricevere l'evento
     * e il dispatcher deve restare attivo.
     */
    @Test
    void unListenerInErroreNonBloccaGliAltri() throws Exception {
        CountDownLatch ricevuti = new CountDownLatch(2);
        dispatcher.aggiungi(e -> { throw new RuntimeException("errore di prova"); });
        dispatcher.aggiungi(e -> ricevuti.countDown());

        registro.pubblica(evento("Alysa"));
        registro.pubblica(evento("Kimi"));

        assertTrue(ricevuti.await(5, TimeUnit.SECONDS));
        assertTrue(thread.isAlive());
    }

    /**
     * Interrompere il thread del dispatcher deve farlo terminare in modo pulito.
     */
    @Test
    void interrompereIlThreadLoFaTerminare() throws Exception {
        thread.interrupt();
        thread.join(5000);

        assertFalse(thread.isAlive());
    }
}
