package laboratorio.modello;

/**
 * Rappresenta una richiesta di prestito visuaizzabile dall'interfaccia grafica.
 * 
 * Contiene una copia dei dati principali della richiesta: utente, tipo di strumento richiesto, stato corrente
 * ed eventuale strumento assegnato.
 * 
 * @author Runtime Terrors
 * @version 1.0
 */
public record RichiestaView(
    String utente, 
    TipoStrumento tipo, 
    StatoRichiesta stato, 
    String codiceStrumento
) {
 
}