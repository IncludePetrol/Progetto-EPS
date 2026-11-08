package laboratorio.modello;

/**
 * Rappresenta i possibili stati di una richiesta di prestito.
 * 
 * Una richiesta può essere in attesa di uno strumento, in prestito, conclusa con la restituzione o annullata.
 * 
 * @author Runtime Terrors
 * @version 1.0
 */
public enum StatoRichiesta {
    
    /** 
     * La richiesta è in attesa di uno strumento disponibile.
     */
    IN_ATTESA,
    
    /** 
     * Lo strumento è stato assegnato e il prestito è in corso.
     */
    IN_PRESTITO,
    
    /** 
     * Lo strumento è stato restituito e la richiesta è terminata.
     */
    RESTITUITA,
    
    /** 
     * La richiesta è stata annullata. 
     */
    ANNULLATA
}