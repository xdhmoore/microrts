package util;

import java.util.Objects;

public class Pair<T1,T2> {
	public T1 m_a;
	public T2 m_b;
	
	public Pair(T1 a,T2 b) {
		m_a = a;
		m_b = b;
	}   
        
    public String toString() {
        return "<" + m_a + "," + m_b + ">";
    }
    
    @Override
    public boolean equals(Object obj) {
    	if (obj == null) return false;
    	if (!(obj instanceof Pair<?, ?>)) return false;
    	Pair<?, ?> otherPair = (Pair<?, ?>) obj;
    	return Objects.equals(m_a, otherPair.m_a) &&
    		Objects.equals(m_b, otherPair.m_b);
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(m_a, m_b);
    }
}
