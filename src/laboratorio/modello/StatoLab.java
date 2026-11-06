package laboratorio.modello;

import java.util.List;

/**
 * <h1>Record StatoLab</h1>
 * 
 * Una vera e propria "fotografia" istantanea dello stato generale del laboratorio in un dato momento. 
 * Pensa a questo oggetto come a un'istantanea globale che cattura in un unico colpo d'occhio 
 * l'elenco di tutti gli {@link StrumentoView} presenti e lo storico o lo stato di tutte le 
 * {@link RichiestaView}.
 * 
 * <p>Viene utilizzata principalmente per nutrire l'interfaccia grafica (GUI) o per gestire il 
 * salvataggio dello stato. Oltre a custodire i dati in modo completamente immutabile (grazie 
 * all'uso dei {@code record}), mette a disposizione metodi di comodo basati sugli Stream di Java 
 * per calcolare rapidamente statistiche al volo, come quanti strumenti sono liberi o quante persone 
 * sono in coda ad aspettare.</p>
 * 
 * @author Runtime Terrors
 * @version 1.0
 */
public record StatoLab(
    List<StrumentoView> strumenti, 
    List<RichiestaView> richieste
) {

    /**
     * Calcola quanti strumenti sono attualmente liberi, pronti per essere presi in prestito 
     * dagli utenti nel laboratorio.
     * 
     * @return Il numero di strumenti con stato {@link StatoStrumento#DISPONIBILE}
     */
    public long disponibili() {
        return strumenti.stream().filter(s -> s.stato() == StatoStrumento.DISPONIBILE).count();
    }

    /**
     * Calcola quanti strumenti si trovano attualmente fuori dal laboratorio perché impegnati 
     * in un prestito attivo.
     * 
     * @return Il numero di strumenti con stato {@link StatoStrumento#IN_PRESTITO}
     */
    public long inPrestito() {
        return strumenti.stream().filter(s -> s.stato() == StatoStrumento.IN_PRESTITO).count();
    }

    /**
     * Calcola quante richieste di prestito sono attualmente in sospeso, con gli utenti 
     * in coda che attendono che si liberi l'attrezzatura desiderata.
     * 
     * @return Il numero di richieste con stato {@link StatoRichiesta#IN_ATTESA}
     */
    public long inAttesa() {
        return richieste.stream().filter(r -> r.stato() == StatoRichiesta.IN_ATTESA).count();
    }
}