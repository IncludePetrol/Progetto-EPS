package laboratorio.modello;

import java.util.List;

/**
 * Rappresenta lo stato corrente del laboratorio.
 * 
 * Contiene l'elenco degli strumenti e delle richieste presenti in un determinato momento.
 * 
 * Viene usato principalmente per fornire i dati all'interfaccia grafica o per operazioni di salvataggio.
 * 
 * Include alcuni metodi che permettono di ottenere informazioni sullo stato del laboratorio, es. numero di 
 * strumenti disponibili, in prestito o le richieste in attesa.
 * 
 * @author Runtime Terrors
 * @version 1.0
 */
public record StatoLab(
    List<StrumentoView> strumenti, 
    List<RichiestaView> richieste
) {

    /**
     * Restituisce il numero di strumenti attualmente disponibili.
     * 
     * @return Il numero di strumenti con stato {@link StatoStrumento#DISPONIBILE}
     */
    public long disponibili() {
        return strumenti.stream().filter(s -> s.stato() == StatoStrumento.DISPONIBILE).count();
    }

    /**
     * Restituisce il numero di strumenti attualmente in prestito.
     * 
     * @return Il numero di strumenti con stato {@link StatoStrumento#IN_PRESTITO}
     */
    public long inPrestito() {
        return strumenti.stream().filter(s -> s.stato() == StatoStrumento.IN_PRESTITO).count();
    }

    /**
     * Restituisce il numero di richieste attualmente in attesa.
     * 
     * @return Il numero di richieste con stato {@link StatoRichiesta#IN_ATTESA}
     */
    public long inAttesa() {
        return richieste.stream().filter(r -> r.stato() == StatoRichiesta.IN_ATTESA).count();
    }
}