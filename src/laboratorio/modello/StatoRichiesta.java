package laboratorio.modello;

/**
 * <h1>Enum StatoRichiesta</h1>
 * 
 * Descrive le possibili fasi o "stati" che una richiesta di prestito attraversa 
 * all'interno del ciclo di vita gestito dal laboratorio. 
 * 
 * <p>Pensa a questo elenco come ai timbri su un modulo: una richiesta nasce in coda 
 * in attesa di disponibilità, passa allo stato di prestito attivo quando lo strumento 
 * viene effettivamente consegnato, e si conclude regolarmente con la restituzione 
 * oppure con un annullamento (ad esempio se l'utente interrompe l'attesa).</p>
 * 
 * @author Runtime Terrors
 * @version 1.0
 */
public enum StatoRichiesta {
    
    /** 
     * L'utente è in coda e sta aspettando che si liberi un esemplare dello strumento richiesto. 
     */
    IN_ATTESA,
    
    /** 
     * Lo strumento è stato assegnato con successo e il prestito è attualmente in corso. 
     */
    IN_PRESTITO,
    
    /** 
     * Lo strumento è stato regolarmente riconsegnato in laboratorio e la richiesta è completata. 
     */
    RESTITUITA,
    
    /** 
     * La richiesta è stata annullata (solitamente a causa dell'interruzione del thread in attesa). 
     */
    ANNULLATA
}