package fa.nfa;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import fa.State;

/**
 * @author Nathan Maroko
 * Date: 11/13/2021
 * Desc: This class defines a State for a Non-Deterministic Finite Automata
 */
public class NFAState extends State{

    private boolean isFinal;
    private HashMap<Character,Set<NFAState>> delta;

    public NFAState(boolean isFinal, String name){
        this.isFinal = isFinal;
        this.name = name;
        this.delta = new HashMap<Character,Set<NFAState>>();
    }

    public NFAState(String name){
        this.name = name;
        this.delta = new HashMap<Character,Set<NFAState>>();
    }

    public boolean getIsFinal(){
        return this.isFinal;
    }

    public void addTransition(char onSymb, NFAState to){
        if(delta.containsKey(onSymb)){
            Set<NFAState> existing = delta.get(onSymb);
            existing.add(to);
            this.delta.put(onSymb, existing);
        }else{
            Set<NFAState> newSet = new LinkedHashSet<NFAState>();
            newSet.add(to);
            this.delta.put(onSymb, newSet);
        }
    }

    public Set<NFAState> getToStates(char onSymb){
        if(!delta.containsKey(onSymb)){
            return null;
        }else{
            return this.delta.get(onSymb);
        }
    }

    
}
