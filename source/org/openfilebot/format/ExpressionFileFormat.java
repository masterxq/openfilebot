package org.openfilebot.format;

import static org.openfilebot.similarity.Normalization.*;
import static org.openfilebot.util.FileUtilities.*;
import static org.openfilebot.util.RegularExpressions.*;

import javax.script.Bindings;
import javax.script.ScriptException;

public class ExpressionFileFormat extends ExpressionFormat {

	public ExpressionFileFormat(String expression) throws ScriptException {
		super(expression);
	}

	@Override
	public Bindings getBindings(Object value) {
		return new ExpressionBindings(value) {

			@Override
			public Object get(Object key) {
				return normalizeBindingValue(super.get(key));
			}
		};
	}

	protected Object normalizeBindingValue(Object value) {
		// if the binding value is a String, then remove illegal characters (that would insert accidental directory separators)
		if (value instanceof CharSequence) {
			return replacePathSeparators(value.toString(), " ");
		}

		// if the binding value is an Object, just leave it
		return value;
	}

	@Override
	protected String normalizeResult(CharSequence value) {
		// normalize unicode space characters and remove newline characters
		return normalizePathSeparators(replaceSpace(stripCRLF(value), " ").trim());
	}

	protected String stripCRLF(CharSequence value) {
		return NEWLINE.matcher(value).replaceAll("");
	}

}
