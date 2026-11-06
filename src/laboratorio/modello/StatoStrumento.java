package laboratorio.modello;

/**
 * Definisce lo stato di disponibilità di uno strumento all'interno del laboratorio.
 */
public enum StatoStrumento {
    /**
     * Lo strumento è fisicamente presente in laboratorio, libero e pronto per un nuovo prestito.
     */
    DISPONIBILE,
    /**
     * Lo strumento è attualmente in uso da un utente e non può essere riassegnato 
     * finché non viene restituito.
     */
    IN_PRESTITO
}
