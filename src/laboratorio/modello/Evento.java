package laboratorio.modello;

import java.time.LocalTime;

/**
 * <h1>Classe Evento</h1>
 * 
 * Rappresenta il singolo "fatto" o movimento registrato all'interno del sistema di laboratorio. 
 * Pensa a un evento come a una riga di un registro storico: ci dice <b>quando</b> è successo 
 * qualcosa (l'orario), <b>cosa</b> è successo (il tipo di evento), <b>chi</b> l'ha compiuto 
 * (l'utente) e <b>quale attrezzatura</b> è stata coinvolta (lo strumento).
 * 
 * <p>Essendo implementata come {@code record} di Java, questa classe è leggera, immutabile 
 * e nasce principalmente per trasportare dati in modo pulito e sicuro, mettendoci a disposizione 
 * anche un metodo furbo per tradurre lo stato in una frase di senso compiuto.</p>
 * 
 * @author Runtime Terrors
 * @version 1.0
 */
public record Evento(LocalTime orario, TipoEvento tipo, String utente, String strumento) {

    /**
     * Traduce l'evento tecnico in una frase discorsiva, leggibile e pronta per essere 
     * mostrata a schermo nell'interfaccia o nei log di sistema.
     * 
     * <p>Sfrutta un'istruzione {@code switch} espressiva per mappare ogni {@link TipoEvento} 
     * in una descrizione testuale naturale, combinando il nome dell'utente e dello strumento.</p>
     * 
     * @return Una stringa descrittiva dell'azione (es. "Mario Rossi ottiene Multimetro 01")
     */
    public String descrizione() {
        return switch (tipo) {
            case ASSEGNAZIONE -> utente + " ottiene " + strumento;
            case ATTESA -> utente + " in attesa di " + strumento;
            case RESTITUZIONE -> utente + " restituisce " + strumento;
            case ANNULLAMENTO -> utente + " annulla la richiesta di " + strumento;
        };
    }
}