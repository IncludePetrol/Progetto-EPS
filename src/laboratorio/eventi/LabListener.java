package laboratorio.eventi;

import laboratorio.modello.Evento;

/**
 * Interfaccia implementata dai componenti che desiderano ricevere
 * notifiche sugli eventi generati dal laboratorio.
 *
 * Le notifiche vengono inviate dal {@link DispatcherEventi}
 * ogni volta che viene pubblicato un nuovo {@link Evento}.
 */
public interface LabListener {

    /**
     * Gestisce un evento ricevuto dal dispatcher.
     *
     * @param e l'evento da elaborare
     */
    void onEvento(Evento e);
}