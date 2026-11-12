package laboratorio.eventi;

import laboratorio.core.RegistroEventi;
import laboratorio.modello.Evento;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestisce la distribuzione degli eventi generati dal laboratorio.
 *
 * Questa classe viene eseguita su un thread separato e rimane in attesa
 * di nuovi eventi pubblicati nel {@link RegistroEventi}. Quando ne trova,
 * li recupera e li inoltra a tutti i {@link LabListener} registrati.
 *
 * Per evitare problemi di concorrenza durante le notifiche, i listener
 * vengono copiati in una lista temporanea prima dell'iterazione. In questo
 * modo eventuali modifiche alla lista originale non interferiscono con il
 * processo di notifica.
 *
 */

public class DispatcherEventi implements Runnable {

    /** Registro condiviso da cui vengono recuperati gli eventi da notificare. */
    private final RegistroEventi registro;

    /** Elenco dei listener registrati per la ricezione degli eventi. */
    private final List<LabListener> listeners = new ArrayList<>();

    /**
     * Crea un nuovo dispatcher associato al registro degli eventi.
    *
    * @param registro il registro da cui prelevare gli eventi
    */
    public DispatcherEventi(RegistroEventi registro) {
        this.registro = registro;
    }

    /**
    * Registra un nuovo listener che verrà notificato alla pubblicazione
    * dei futuri eventi.
    *
    * @param l listener da aggiungere
    */
    public synchronized void aggiungi(LabListener l) {
        listeners.add(l);
    }

    /**
    * Rimuove un listener precedentemente registrato.
    *
    * @param l listener da rimuovere
    */
    public synchronized void rimuovi(LabListener l) {
        listeners.remove(l);
    }

   /*** * Esegue il ciclo principale del *ispatcher.
    *
    * Il thread rimane in attesa di nuovi eventi nel registro e,
    * quando vengono pubblicati li inoltra a tutti i listener
    * registrati. Le eventuali eccezioni generate da un listener
    * vengono gestite senza interrompere la notifica degli altri.
    */
    @Override
    public void run() {
        while (true) {
            List<Evento> eventi;
            try {
                eventi = registro.preleva(); 
            } catch (InterruptedException e) {
                return;
            }

            
            List<LabListener> copia;
            synchronized (this) {
                copia = new ArrayList<>(listeners);
            }

            for (Evento e : eventi) {
                for (LabListener l : copia) {
                    try {
                        l.onEvento(e);
                    } catch (RuntimeException ex) {
                        System.err.println("listener in errore: " + ex);
                    }
                }
            }
        }
    }
}
