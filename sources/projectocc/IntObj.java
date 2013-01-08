/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package projectocc;

/**
 *
 * @author Miguel
 */
public class IntObj {
    public int value;
    
    public synchronized int get(){
        return value;
    }
    
    public synchronized void set(int x){
        this.value = x;
    }
    
}
