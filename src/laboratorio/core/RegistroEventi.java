package laboratorio.core;

import laboratorio.modello.Evento;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe responsabile della gestione degli eventi prodotti dal sistema.
 * 
 * Conserva lo storico completo degli eventi e la coda degli eventi in attesa di essere notificati.
 * 
 * I metodi che accedono alle strutture interne sono sincronizzati per garantire un accesso sicuro ai dati
 * condivisi tra thread concorrenti.
 * 
 * Per evitare deadlock e problemi di concorrenza i lock vengono acquisiti seguendo l'ordine
 * Laboratorio -> RegistroEventi.
 * 
 * @author Runtime Terrors
 */
public class RegistroEventi {

    /** 
     * Lista contenente gli eventi generati ma ancora da notificare 
     */

    private final List<Evento> daNotificare = new ArrayList<>();
    /** 
     * Registro permanente che contiene lo storico completo degli eventi generati durante la simulazione. 
     */
    private final List<Evento> storico = new ArrayList<>();

    /**
     * Aggiunge un nuovo evento sia allo storico degli eventi sia alla lista degli eventi in attesa di notifica.
     * Al termine dell'inserimento, notifica eventuali thread in attesa.
     *
     * @param e l'evento da aggiungere allo storico
     */
        public synchronized void pubblica(Evento e) {
        storico.add(e);
        daNotificare.add(e);
        notifyAll();
    }

    /**
     * Restituisce tutti gli eventi presenti nella coda di notifica.
     * 
     * Se la coda {@code daNotificare} è vuota, il thread viene posto in stato di attesa
     * mediante la guardia {@code while (daNotificare.isEmpty()) wait()} finchè non viene aggiunto un nuovo evento. 
     * In questo modo si evita il busy waiting e si riduce l'utilizzo della CPU.
     *
     * @return una copia degli eventi da notificare.
     * @throws InterruptedException se il thread in attesa viene interrotto
     */
    public synchronized List<Evento> preleva() throws InterruptedException {
        while (daNotificare.isEmpty()) {
            wait();
        }
        List<Evento> out = List.copyOf(daNotificare);
        daNotificare.clear();
        return out;
    }

    /**
     * Restituisce una copia immutabile dello storico completo degli eventi registrati fino al momento della chiamata.
     *
     * @return una lista unmodifiable contenente la cronologia completa degli eventi
     */
    public synchronized List<Evento> getStorico() {
        return List.copyOf(storico);
    }

    /**
     * Sostituisce lo storico corrente con quello passato come parametro, generalmente caricato da un file salvato in precedenza.
     * Svuota la coda delle notifiche e notifica eventuali thread in attesa.
     *
     * @param nuovoStorico la lista di eventi da usare come nuovo storico
     */
    public synchronized void ripristina(List<Evento> nuovoStorico) {
        storico.clear();
        storico.addAll(nuovoStorico);
        daNotificare.clear();
        notifyAll();
    }
}
