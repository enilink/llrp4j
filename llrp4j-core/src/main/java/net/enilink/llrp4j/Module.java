package net.enilink.llrp4j;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Module {
	protected Set<Class<?>> classes = new LinkedHashSet<>();
	protected Map<String, String> namespaces = new HashMap<>();

	public Module() {
	}

	public Module(Set<Class<?>> classes) {
		this.classes.addAll(classes);
	}

	public Module addNamespace(String prefix, String uri) {
		namespaces.put(prefix, uri);
		return this;
	}

	public Module addClass(Class<?> clazz) {
		this.classes.add(clazz);
		return this;
	}

	public Module addClasses(Class<?>... classes) {
		for (Class<?> clazz : classes) {
			this.classes.add(clazz);
		}
		return this;
	}

	public Module addClasses(Collection<Class<?>> classes) {
		this.classes.addAll(classes);
		return this;
	}

	public Module include(Module other) {
		this.classes.addAll(other.classes);
		return this;
	}

	public Set<Class<?>> getClasses() {
		return Collections.unmodifiableSet(classes);
	}

	public Map<String, String> getNamespaces() {
		return Collections.unmodifiableMap(namespaces);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classes == null) ? 0 : classes.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Module other = (Module) obj;
		if (classes == null) {
			if (other.classes != null)
				return false;
		} else if (!classes.equals(other.classes))
			return false;
		return true;
	}
}
