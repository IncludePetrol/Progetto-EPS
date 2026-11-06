package laboratorio.core;

import laboratorio.modello.Evento;

import java.util.ArrayList;
import java.util.List;

// monitor che tiene lo storico degli eventi e la coda di quelli da notificare
// il Laboratorio lo chiama dentro i propri metodi (ordine dei lock Laboratorio -> RegistroEventi)
public class RegistroEventi {

    private final List<Evento> daNotificare = new ArrayList<>();
    private final List<Evento> storico = new ArrayList<>();

    // aggiunge un evento sia allo storico sia alla coda
    public synchronized void pubblica(Evento e) {
        storico.add(e);
        daNotificare.add(e);
        notifyAll();
    }

    // aspetta che ci sia qualcosa da notificare e lo prende tutto
    public synchronized List<Evento> preleva() throws InterruptedException {
        while (daNotificare.isEmpty()) {
            wait();
        }
        List<Evento> out = List.copyOf(daNotificare);
        daNotificare.clear();
        return out;
    }

    public synchronized List<Evento> getStorico() {
        return List.copyOf(storico);
    }

    // usato al caricamento da file
    public synchronized void ripristina(List<Evento> nuovoStorico) {
        storico.clear();
        storico.addAll(nuovoStorico);
        daNotificare.clear();
        notifyAll();
    }
}
