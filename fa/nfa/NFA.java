package fa.nfa;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import fa.State;
import fa.dfa.DFA;

public class NFA implements NFAInterface{

    private Set<NFAState> states;
	private NFAState start;
	private Set<Character> ordAbc;
    private Set<NFAState> eClosure;
    private Set<NFAState> finalStates;

    private boolean containsEmpty;

    public NFA(){
        this.states = new LinkedHashSet<NFAState>();
        this.ordAbc = new LinkedHashSet<Character>();
        this.finalStates = new LinkedHashSet<NFAState>();
        this.containsEmpty = false;
    }

    @Override
    public void addStartState(String name) {
        NFAState s = checkIfExists(name);
        if(s == null){
            this.start = new NFAState(name);
            this.states.add(this.start);
        }else{
            System.out.println("WARNING: A state with name " + name + " already exists in the DFA");
        }
    }

    @Override
    public void addState(String name) {
        NFAState s = checkIfExists(name);
        if(s == null){
            this.states.add(new NFAState(name));
        }else{
            System.out.println("WARNING: A state with name " + name + " already exists in the DFA");
        }
    }

    @Override
    public void addFinalState(String name) {
        NFAState s = checkIfExists(name);
        if(s == null){
            NFAState temp = new NFAState(true, name);
            this.states.add(temp);
            this.finalStates.add(temp);
        }else{
            System.out.println("WARNING: A state with name " + name + " already exists in the DFA");
        } 
    }

    @Override
    public void addTransition(String fromState, char onSymb, String toState) {
        NFAState from = checkIfExists(fromState);
		NFAState to = checkIfExists(toState);
		if(from == null){
			System.err.println("ERROR: No DFA state exists with name " + fromState);
			System.exit(2);
		} else if (to == null){
			System.err.println("ERROR: No DFA state exists with name " + toState);
			System.exit(2);
		}
        from.addTransition(onSymb, to);

        if(!ordAbc.contains(onSymb)){
            ordAbc.add(onSymb);
        }
        
    }

    @Override
    public Set<? extends State> getStates() {
        
        return this.states;
    }

    @Override
    public Set<? extends State> getFinalStates() {
        Set<NFAState> finals = new LinkedHashSet<NFAState>();
        for(NFAState state: this.states){
            if(state.getIsFinal()){
                finals.add(state);
            }
        }
        return finals;
    }

    @Override
    public State getStartState() {
        return this.start;
    }

    @Override
    public Set<Character> getABC() {

        return this.ordAbc;
    }

    @Override
    public DFA getDFA() {
        Map<NFAState,Set<NFAState>> eClosures = new HashMap<NFAState,Set<NFAState>>(); //Keep track of eClosures of each state
        Set<Set<NFAState>> includedStates = new LinkedHashSet<Set<NFAState>>(); //Keep track of what states we have in the new machine
        Queue<Set<NFAState>> queue = new LinkedList<Set<NFAState>>(); //Queue used for the Breadth-First Traversal
        Set<NFAState> visited = new LinkedHashSet<NFAState>(); //Keep track of what states we have visited

        DFA convertedDFA = new DFA();
        NFAState start = checkIfExists(this.start.getName());
        Set<NFAState> eClosureStart = eClosure(start);
        
        convertedDFA.addStartState(eClosureStart.toString());
        includedStates.add(eClosureStart);

        visited.add(start);

        for(NFAState s: this.states){ //Calculate all eClosures
            eClosures.put(s, eClosure(s)); //Map current state to eclosure value
        }


        //Breadth-First Traversal of NFA
        for(Character onSymb: this.ordAbc){ //Get Set of states starting with entry state
            Set<NFAState> toStates = start.getToStates(onSymb);

            Set<NFAState> toStateDFA = new LinkedHashSet<NFAState>();
            for(NFAState current: eClosureStart){ //Get all of the to states from the singletons that make up the currentState
                Set<NFAState> add = current.getToStates(onSymb);
                if(add != null){
                    toStateDFA.addAll(add);
                }
            }

            if(onSymb == 'e'){// If the onSymb is e and null is returned then continue
                continue;
            }else if(toStateDFA.isEmpty() && onSymb != 'e'){ //Else, add the empty state as a transition from the start state
                if(!this.containsEmpty){ //If empty state isn't in machine yet
                    convertedDFA.addState("[]"); //Create empty state in machine
                    convertedDFA = addEmptyTransitions(convertedDFA); //Add transitions for each character on empty state
                    this.containsEmpty = true;

                    convertedDFA.addTransition(eClosureStart.toString(), onSymb, "[]"); //Add transition from current to empty state on the characterSymb
                }else{
                    convertedDFA.addTransition(eClosureStart.toString(), onSymb, "[]");
                }
            }

            Set<NFAState> transition = new LinkedHashSet<NFAState>();
            for(NFAState state: toStateDFA){ //Check each eClosure for each character in the set
                transition.addAll(eClosures.get(state));
            }

            if(includedStates.contains(transition)){ //If the transition state is in DFA already
                transition = getIncludedState(includedStates, transition); //Find the exact state name in machine
                convertedDFA.addTransition(eClosureStart.toString(), onSymb, transition.toString()); //Add a transition from the currentState to transitionState
            }else{ //Transition state is not in DFA
                if(checkIfFinal(transition)){
                    convertedDFA.addFinalState(transition.toString());
                }else{
                    convertedDFA.addState(transition.toString()); //Create the State in the DFA
                }
                includedStates.add(transition); //Add the state to the included states
                convertedDFA.addTransition(eClosureStart.toString(), onSymb, transition.toString()); //Add a transition from the currentState to transitionState
            }
            queue.add(toStates);
        }


        while(!queue.isEmpty()){
            Set<NFAState> s = queue.poll(); //Pull out first set of states in queue
            for(NFAState currentState: s){ //Walk through set of states
                if(!checkIfVisited(visited, currentState)){ //If we haven't visited the currentState
                    visited.add(currentState); //Mark this state as visited

                    //Check if the eClosure of the state is included in the constructed DFA
                    Set<NFAState> currentEClosure = eClosure(currentState);
                    if(includedStates.contains(currentEClosure)){ //If included in DFA already
                        currentEClosure = getIncludedState(includedStates, currentEClosure); //Find the exact string name of the currentEClosure
                    }else{
                        if(checkIfFinal(currentEClosure)){
                            convertedDFA.addFinalState(currentEClosure.toString());
                        }else{
                            convertedDFA.addState(currentEClosure.toString()); //Create the State in the DFA
                        }
                        includedStates.add(currentEClosure);
                    }

                    for(Character onSymb: this.ordAbc){ //Walk through each possible transition on currentState
                        Set<NFAState> toState = currentState.getToStates(onSymb);

                        Set<NFAState> toStateDFA = new LinkedHashSet<NFAState>();
                        for(NFAState current: currentEClosure){ //Get all of the to states from the singletons that make up the currentState
                            Set<NFAState> add = current.getToStates(onSymb);
                            if(add != null){
                                toStateDFA.addAll(add);
                            }
                        }
                        
                        if(toState == null || onSymb == 'e'){// If the onSymb is e and null is returned then continue
                            continue;
                        }else if(toStateDFA.isEmpty() && onSymb != 'e'){ //Else, add the empty state as a transition from the start state
                            if(!this.containsEmpty){ //If empty state isn't in machine yet
                                convertedDFA.addState("[]"); //Create empty state in machine
                                convertedDFA = addEmptyTransitions(convertedDFA); //Add transitions for each character on empty state
                                this.containsEmpty = true;
            
                                convertedDFA.addTransition(currentEClosure.toString(), onSymb, "[]"); //Add transition from current to empty state on the characterSymb
                            }else{
                                convertedDFA.addTransition(currentEClosure.toString(), onSymb, "[]");
                            }
                            continue;
                        }

                        Set<NFAState> transition = new LinkedHashSet<NFAState>();
                        for(NFAState state: toStateDFA){ //Check each eClosure for each character in the set
                            transition.addAll(eClosures.get(state));
                        }
            
                        if(includedStates.contains(transition)){ //If the transition state is in DFA already
                            transition = getIncludedState(includedStates, transition); //Find the exact state name in machine
                            convertedDFA.addTransition(currentEClosure.toString(), onSymb, transition.toString()); //Add a transition from the currentState to transitionState
                        }else{ //Transition state is not in DFA
                            convertedDFA.addState(transition.toString()); //Create the State in the DFA
                            includedStates.add(transition); //Add the state to the included states
                            convertedDFA.addTransition(currentEClosure.toString(), onSymb, transition.toString()); //Add a transition from the currentState to transitionState
                        }

                        queue.add(toState); //Add ToStates to queue
                    }
                }
            }
        }
        // for(NFAState state: eClosures.keySet()){
        //     System.out.println(state + ": " + eClosures.get(state));
        // }

        return convertedDFA;
    }

    @Override
    public Set<NFAState> getToState(NFAState from, char onSymb) {
        return from.getToStates(onSymb);
    }

    @Override
    public Set<NFAState> eClosure(NFAState s) {
        this.eClosure = new LinkedHashSet<NFAState>();
        Set<NFAState> visited = new LinkedHashSet<NFAState>();
        eClosure.add(s);
        eClosureHelper(s, visited);
        return eClosure;
    }

    private void eClosureHelper(NFAState s, Set<NFAState> visited) {
        Set<NFAState> current = s.getToStates('e');
        if(current == null){
            return;
        }
        for(NFAState state : current){
            this.eClosure.add(state);
            if(checkIfVisited(visited, state)){
                eClosureHelper(state, visited);
                visited.add(state);
            }
        }
    }

    private boolean checkIfVisited(Set<NFAState> visited, NFAState state){
        boolean ret = false;
        for(NFAState s : visited){
			if(s.getName().equals(state.getName())){
				ret = true;
				break;
			}
		}
        return ret;
    }

    private NFAState checkIfExists(String name){
		NFAState ret = null;
		for(NFAState s : states){
			if(s.getName().equals(name)){
				ret = s;
				break;
			}
		}
		return ret;
	}

    private DFA addEmptyTransitions(DFA current){
        for(Character c : this.ordAbc){
            if(c != 'e'){
                current.addTransition("[]", c, "[]");
            }
        }
        return current;
    }

    private Set<NFAState> getIncludedState(Set<Set<NFAState>> includedStates, Set<NFAState> set){
        for(Set<NFAState> current: includedStates){
            if(current.equals(set)){
                return current;
            }
        }
        return null;
    }

    private boolean checkIfFinal(Set<NFAState> current){
        boolean retVal = false;
        for(NFAState currentState: this.finalStates){
            if(current.contains(currentState)){
                retVal = true;
                break;
            }
        }
        return retVal;
    }
    
}
