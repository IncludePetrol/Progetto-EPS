package laboratorio.simulazione;

import laboratorio.core.Laboratorio;
import laboratorio.modello.TipoStrumento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// simulazione su più utenti, ognuno su un thread distinto, chiedono strumenti al laboratorio.
// ogni utente fa un certo numero di giri: richiede uno strumento, lo tiene per un tempo
// (la durata del prestito), poi lo restituisce.
// il tempo di prestito è fuori dal monitor: il monitor non viene mai tenuto durante lo sleep.
public class Simulazione {

    public static final int MIN_UTENTI = 5; // requisito: almeno cinque utenti

    // nomi tra cui vengono scelti gli utenti: tutti diversi, perché il laboratorio
    // riconosce chi restituisce uno strumento proprio dal nome
    private static final List<String> NOMI = List.of(
            "Lucy", "David", "Rebecca", "Roman", "Talia",
            "Alysa", "Kimi", "Max", "Asuka", "Rei",
            "Shinji", "Misato", "Kaworu", "Johnny", "Frieren",
            "Stark", "Fern", "Aldo", "Giovanni", "Giacomo");

    public static final int MAX_UTENTI = NOMI.size(); // un nome diverso per ogni utente

    private final Laboratorio lab;
    private final int utenti;
    private final int giri;
    private final long durataPrestitoMs;
    private ExecutorService pool;
    private int attivi; // utenti che stanno ancora girando

    public Simulazione(Laboratorio lab, int utenti, int giri, long durataPrestitoMs) {
        if (utenti < MIN_UTENTI) {
            throw new IllegalArgumentException("servono almeno " + MIN_UTENTI + " utenti");
        }
        if (utenti > MAX_UTENTI) {
            throw new IllegalArgumentException("al massimo " + MAX_UTENTI + " utenti");
        }
        if (giri < 1) {
            throw new IllegalArgumentException("i giri devono essere almeno 1");
        }
        if (durataPrestitoMs < 0) {
            throw new IllegalArgumentException("la durata del prestito non può essere negativa");
        }
        this.lab = lab;
        this.utenti = utenti;
        this.giri = giri;
        this.durataPrestitoMs = durataPrestitoMs;
    }

    // avvia gli utenti, ognuno sul proprio thread
    public synchronized void avvia() {
        if (attivi > 0) {
            throw new IllegalStateException("simulazione già avviata");
        }
        List<TipoStrumento> tipi = new ArrayList<>(lab.tipiPresenti());
        if (tipi.isEmpty()) {
            throw new IllegalStateException("il laboratorio non ha strumenti");
        }

        attivi = utenti;
        pool = Executors.newFixedThreadPool(utenti);
        List<String> nomi = new ArrayList<>(NOMI);
        Collections.shuffle(nomi); // a ogni avvio nomi diversi, senza doppioni
        for (int u = 0; u < utenti; u++) {
            String nome = nomi.get(u);
            int indice = u;
            pool.execute(() -> {
                try {
                    usaStrumenti(nome, indice, tipi);
                } finally {
                    utenteFinito();
                }
            });
        }
        pool.shutdown(); // niente nuovi task: i thread finiscono da soli quando gli utenti hanno chiuso
    }

    private synchronized void utenteFinito() {
        attivi--;
    }

    public synchronized boolean isAttiva() {
        return attivi > 0;
    }

    public synchronized int threadAttivi() {
        return attivi;
    }

    // ogni utente chiede tipi diversi nei vari giri, così i tipi vengono usati tutti
    private void usaStrumenti(String nome, int indice, List<TipoStrumento> tipi) {
        for (int giro = 0; giro < giri; giro++) {
            TipoStrumento tipo = tipi.get((indice + giro) % tipi.size());
            String codice = null;
            try {
                codice = lab.richiedi(nome, tipo); // può aspettare una restituzione
                Thread.sleep(durataPrestitoMs);     // durata del prestito simulata
            } catch (InterruptedException e) {
                return; // simulazione interrotta: il finally restituisce lo strumento se lo aveva
            } finally {
                if (codice != null) {
                    lab.restituisci(nome, codice);
                }
            }
        }
    }

    // ferma la simulazione: interrompe i thread degli utenti (chi aspetta esce dalla coda)
    public synchronized void interrompi() {
        if (pool != null) {
            pool.shutdownNow();
        }
    }

    // aspetta la fine di tutti gli utenti, al massimo per il tempo dato; true se sono finiti
    public boolean attendiFine(long timeoutMs) throws InterruptedException {
        ExecutorService p;
        synchronized (this) {
            p = pool;
        }
        if (p == null) {
            return true;
        }
        p.shutdown();
        return p.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
    }
}
