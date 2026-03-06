
package org.openfilebot.hash;


public interface Hash {

	public void update(byte[] bytes, int off, int len);


	public String digest();

}
