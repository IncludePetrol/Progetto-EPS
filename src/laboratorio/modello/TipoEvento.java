package laboratorio.modello;

/**
 * Rappresenta i diversi tipi di evento registrati durante la gestione dei prestiti.
 */
public enum TipoEvento {
    /** Uno strumento è stato assegnato a un utente. */
    ASSEGNAZIONE,
    /** Un utente è entrato in attesa perché lo strumento non era disponibile. */
    ATTESA,
    /** Uno strumento è stato restituito al laboratorio. */
    RESTITUZIONE,
    /** Una richiesta in attesa è stata annullata. */
    ANNULLAMENTO
}
