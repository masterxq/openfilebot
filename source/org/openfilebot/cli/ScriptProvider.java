package org.openfilebot.cli;

public interface ScriptProvider {

	String getScript(String name) throws Exception;

}