package laboratorio.modello;

import java.time.LocalTime;

/**
 * Rappresenta un singolo evento generato nel sistema.
 * 
 * Ogni evento contiene l'orario in cui è avvenuto, il tipo di operazione eseguita, l'utente coinvolto e 
 * lo strumento usato.
 * 
 * La classe è implementata come record, quindi i suoi dati sono immutabili e vengono usati principalmente per 
 * memorizzare e trasferire le informazioni relative agli eventi generati dal laboratorio.
 * 
 * @author Runtime Terrors
 * @version 1.0
 */
public record Evento(LocalTime orario, TipoEvento tipo, String utente, String strumento) {

    /**
     * Genera una descrizione leggibile dell'evento: in base al tipo di evento viene generata una frase contenente
     * il nome dell'utente e dello strumento coinvolto.
     * 
     * @return una descrizione dell'evento 
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