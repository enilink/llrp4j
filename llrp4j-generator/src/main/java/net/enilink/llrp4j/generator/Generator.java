package net.enilink.llrp4j.generator;

import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.llrp.ltk.schema.core.AllowedInParameterReference;
import org.llrp.ltk.schema.core.Annotation;
import org.llrp.ltk.schema.core.ChoiceDefinition;
import org.llrp.ltk.schema.core.ChoiceParameterReference;
import org.llrp.ltk.schema.core.ChoiceReference;
import org.llrp.ltk.schema.core.CustomChoiceDefinition;
import org.llrp.ltk.schema.core.CustomEnumerationDefinition;
import org.llrp.ltk.schema.core.CustomMessageDefinition;
import org.llrp.ltk.schema.core.CustomParameterDefinition;
import org.llrp.ltk.schema.core.Description;
import org.llrp.ltk.schema.core.Documentation;
import org.llrp.ltk.schema.core.EnumerationDefinition;
import org.llrp.ltk.schema.core.EnumerationEntryDefinition;
import org.llrp.ltk.schema.core.FieldDefinition;
import org.llrp.ltk.schema.core.LlrpDefinition;
import org.llrp.ltk.schema.core.MessageDefinition;
import org.llrp.ltk.schema.core.NamespaceDefinition;
import org.llrp.ltk.schema.core.ParameterDefinition;
import org.llrp.ltk.schema.core.ParameterReference;
import org.llrp.ltk.schema.core.ReservedDefinition;
import org.llrp.ltk.schema.core.VendorDefinition;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.AbstractJType;
import com.helger.jcodemodel.EClassType;
import com.helger.jcodemodel.JAnnotationArrayMember;
import com.helger.jcodemodel.JAnnotationUse;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JClassAlreadyExistsException;
import com.helger.jcodemodel.JCodeModel;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JDocComment;
import com.helger.jcodemodel.JEnumConstant;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JPackage;
import com.helger.jcodemodel.JPrimitiveType;
import com.helger.jcodemodel.JSwitch;
import com.helger.jcodemodel.JVar;
import com.helger.jcodemodel.writer.FileCodeWriter;
import com.helger.jcodemodel.writer.OutputStreamCodeWriter;

import net.enilink.llrp4j.Module;
import net.enilink.llrp4j.annotations.AllowedIn;
import net.enilink.llrp4j.annotations.LlrpCustomMessageType;
import net.enilink.llrp4j.annotations.LlrpCustomParameterType;
import net.enilink.llrp4j.annotations.LlrpField;
import net.enilink.llrp4j.annotations.LlrpMessageType;
import net.enilink.llrp4j.annotations.LlrpNamespace;
import net.enilink.llrp4j.annotations.LlrpParam;
import net.enilink.llrp4j.annotations.LlrpParameterType;
import net.enilink.llrp4j.annotations.LlrpProperties;
import net.enilink.llrp4j.types.LlrpEnum;
import net.enilink.llrp4j.types.LlrpMessage;
import net.enilink.llrp4j.types.Types;

public class Generator {

	static class AllowedInInfo {
		final JDefinedClass parameterClass;
		final JAnnotationUse parameterAnnotation;
		final List<AllowedInParameterReference> allowedIn;

		public AllowedInInfo(JDefinedClass parameterClass, JAnnotationUse parameterAnnotation,
				List<AllowedInParameterReference> allowedIn) {
			this.parameterClass = parameterClass;
			this.parameterAnnotation = parameterAnnotation;
			this.allowedIn = allowedIn;
		}
	}

	protected String packagePrefix = "org.llrp.";

	protected JCodeModel codeModel = new JCodeModel();

	protected List<JCodeModel> searchModels;

	protected List<AllowedInInfo> allowedInRefs = new ArrayList<>();

	protected List<NamespaceDefinition> namespaces = new ArrayList<>();

	protected TransformerFactory tf = TransformerFactory.newInstance();
	protected Transformer transformer;

	public Generator(List<JCodeModel> baseCodeModels)
			throws TransformerConfigurationException, ParserConfigurationException {
		this.searchModels = new ArrayList<>(baseCodeModels);
		this.searchModels.add(codeModel);
		transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "html");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "no");
	}

	public static void main(String[] args) throws Exception {
		List<Path> definitionFiles = new ArrayList<>();
		Path outputPath = null;
		for (String arg : args) {
			Path p = Paths.get(arg);
			if (Files.isRegularFile(p)) {
				definitionFiles.add(p);
			} else if (Files.isDirectory(p)) {
				outputPath = p;
			}
		}
		JAXBContext context = JAXBContext.newInstance(LlrpDefinition.class.getPackage().getName(),
				LlrpDefinition.class.getClassLoader());
		Unmarshaller unmarshaller = context.createUnmarshaller();

		List<JCodeModel> codeModels = new ArrayList<>();
		for (Path p : definitionFiles) {
			JAXBElement<?> element = (JAXBElement<?>) unmarshaller.unmarshal(p.toFile());
			LlrpDefinition definition = (LlrpDefinition) element.getValue();

			Generator generator = new Generator(codeModels);
			generator.processDefinition(definition);

			generator.generateCustomParameterAnnotations();
			generator.generateGettersAndSetters();
			generator.generateHashCodeAndEquals();
			generator.addNamespaces();
			generator.generateModules();

			if (outputPath != null) {
				generator.getCodeModel().build(new FileCodeWriter(outputPath.toFile(), Charset.forName("UTF-8")));
			} else {
				generator.getCodeModel().build(new OutputStreamCodeWriter(System.out, Charset.forName("UTF-8")));
			}

			codeModels.add(generator.codeModel);
		}
	}

	private void addNamespaces() throws Exception {
		String namespace = namespaces.isEmpty() ? "http://www.llrp.org/ltk/schema/core/encoding/xml/1.0"
				: namespaces.get(0).getURI();
		for (Iterator<JPackage> it = codeModel.packages(); it.hasNext();) {
			JPackage p = it.next();
			if (p.name().endsWith("modules") || p.name().endsWith("interfaces")) {
				continue;
			}
			for (JDefinedClass c : p.classes()) {
				c.annotate(LlrpNamespace.class).param("value", namespace);
			}
		}

	}

	private void generateModules() throws Exception {
		String moduleName = namespaces.isEmpty() ? "llrp" : namespaces.get(0).getPrefix();

		JDefinedClass moduleClass = codeModel
				._class(packagePrefix + ("llrp".equals(moduleName) ? "" : moduleName.toLowerCase() + ".") + "modules."
						+ firstUpper(moduleName) + "Module");
		moduleClass._extends(Module.class);

		for (NamespaceDefinition nd : namespaces) {
			moduleClass.instanceInit().invoke("addNamespace").arg(nd.getPrefix().toLowerCase()).arg(nd.getURI());
		}
		for (Iterator<JPackage> it = codeModel.packages(); it.hasNext();) {
			JPackage p = it.next();
			if (p.name().endsWith("modules") || p.name().endsWith("interfaces")) {
				continue;
			}
			for (JDefinedClass c : p.classes()) {
				moduleClass.instanceInit().invoke("addClass").arg(JExpr.dotclass(c));
			}
		}
	}

	public JCodeModel getCodeModel() {
		return codeModel;
	}

	public void generateCustomParameterAnnotations() {
		String[] searchPackages = new String[] { customPkg() + ".messages", customPkg() + ".parameters",
				customPkg() + ".interfaces", "messages", "parameters", "interfaces" };
		for (AllowedInInfo allowedInInfo : allowedInRefs) {
			JAnnotationArrayMember allowedInArray = allowedInInfo.parameterAnnotation.paramArray("allowedIn");
			for (AllowedInParameterReference allowedIn : allowedInInfo.allowedIn) {
				AbstractJClass targetClass = findClass(allowedIn.getType(), searchPackages);
				if (targetClass != null) {
					Repeat repeat = Repeat.parse(allowedIn.getRepeat());
					String name = allowedIn.getName();
					boolean multiple = repeat == Repeat.R0_TO_N || repeat == Repeat.R1_TO_N;
					boolean required = repeat == Repeat.R1 || repeat == Repeat.R1_TO_N;

					JAnnotationUse annotation = allowedInArray.annotate(AllowedIn.class);
					annotation.param("targetType", targetClass);
					if (multiple) {
						annotation.param("multiple", multiple);
					}
					if (required) {
						annotation.param("required", required);
					}
					if (name != null) {
						annotation.param("name", name);
					}
				}
			}
		}
	}

	String customPkg() {
		return namespaces.isEmpty() ? "custom" : namespaces.get(0).getPrefix().toLowerCase();
	}

	AbstractJClass findClass(String name, String pkg, boolean custom) {
		if (custom) {
			return findClass(name, pkg, customPkg() + "." + pkg);
		} else {
			return findClass(name, pkg);
		}
	}

	AbstractJClass findClass(String name, String... packages) {
		for (String suffix : packages) {
			String fqn = packagePrefix + suffix + "." + name;
			// search known models
			for (JCodeModel cm : searchModels) {
				JDefinedClass _class = cm._getClass(fqn);
				if (_class != null) {
					return _class;
				}
			}
		}
		// search class path
		for (String suffix : packages) {
			String fqn = packagePrefix + suffix + "." + name;
			try {
				Class<?> c = getClass().getClassLoader().loadClass(fqn);
				return codeModel.ref(c);
			} catch (ClassNotFoundException e) {
				// ignore
			}
		}
		return null;
	}

	JDefinedClass getOrCreateClass(String suffix, String name, boolean custom, EClassType classType) {
		String pkg = packagePrefix;
		String fqn = pkg + suffix + "." + name;
		JDefinedClass _class = null;
		for (JCodeModel cm : searchModels) {
			_class = cm._getClass(fqn);
			if (_class == null && custom) {
				fqn = pkg + customPkg() + "." + suffix + "." + name;
				_class = cm._getClass(fqn);
			}
			if (_class != null) {
				return _class;
			}
		}
		if (_class == null) {
			try {
				_class = codeModel._class(fqn, classType);
			} catch (JClassAlreadyExistsException e) {
				throw new RuntimeException(e);
			}
		}
		return _class;
	}

	JDefinedClass messageClass(String name, boolean custom) {
		return getOrCreateClass("messages", name, custom, EClassType.CLASS);
	}

	JDefinedClass enumClass(String name, boolean custom) {
		return getOrCreateClass("enumerations", name, custom, EClassType.ENUM);
	}

	AbstractJClass enumClassRef(String name, boolean custom) {
		AbstractJClass c = findClass(name, "enumerations", custom);
		return c != null ? c : enumClass(name, custom);
	}

	JDefinedClass parameterClass(String name, boolean custom) {
		return getOrCreateClass("parameters", name, custom, EClassType.CLASS);
	}

	AbstractJClass parameterClassRef(String name, boolean custom) {
		custom &= !"custom".equalsIgnoreCase(name);
		AbstractJClass c = findClass(name, "parameters", custom);
		return c != null ? c : parameterClass(name, custom);
	}

	JDefinedClass interfaceClass(String name, boolean custom) {
		return getOrCreateClass("interfaces", name, custom, EClassType.INTERFACE);
	}

	AbstractJClass interfaceClassRef(String name, boolean custom) {
		AbstractJClass c = findClass(name, "interfaces", custom);
		return c != null ? c : interfaceClass(name, custom);
	}

	void addProperties(JDefinedClass _class, List<String> properties) {
		_class.annotate(LlrpProperties.class).paramArray("value", properties.toArray(new String[properties.size()]));
	}

	void processDefinition(LlrpDefinition definition) throws JClassAlreadyExistsException {
		Map<String, Long> vendors = new HashMap<>();
		for (Object element : definition.getElements()) {
			if (element instanceof VendorDefinition) {
				VendorDefinition d = (VendorDefinition) element;
				String name = d.getName();
				long vendorID = d.getVendorID();
				vendors.put(name, vendorID);
				annotations(d.getAnnotation());
			} else if (element instanceof NamespaceDefinition) {
				namespaces.add((NamespaceDefinition) element);
			}
		}
		for (Object element : definition.getElements()) {
			if (element instanceof MessageDefinition) {
				MessageDefinition d = (MessageDefinition) element;
				String name = d.getName();

				JDefinedClass _class = messageClass(name, false);
				_class._extends(LlrpMessage.class);
				int typeNum = d.getTypeNum();
				JAnnotationUse msgAnnotation = _class.annotate(LlrpMessageType.class).param("typeNum", typeNum);
				if (d.getResponseType() != null) {
					msgAnnotation.param("responseType", messageClass(d.getResponseType(), false));
				}

				if (!d.getAnnotation().isEmpty()) {
					javadoc(_class.javadoc(), d.getAnnotation());
				}
				List<String> properties = new ArrayList<>();
				properties.addAll(fields(_class, msgAnnotation, d.getFieldOrReserved(), false));
				properties.addAll(parameters(_class, d.getParameterOrChoice(), false));
				addProperties(_class, properties);
			} else if (element instanceof ParameterDefinition) {
				ParameterDefinition d = (ParameterDefinition) element;
				String name = d.getName();
				int typeNum = d.getTypeNum();

				int propertyCount = d.getParameterOrChoice().size();
				for (Object fieldOrReserved : d.getFieldOrReserved()) {
					if (fieldOrReserved instanceof FieldDefinition) {
						propertyCount++;
					}
				}
				if (propertyCount == 1) {
					// TODO these elements may be directly inlined
					// System.out.println(d.getName() + " " + typeNum);
				}

				JDefinedClass _class = parameterClass(name, false);
				if (!d.getAnnotation().isEmpty()) {
					javadoc(_class.javadoc(), d.getAnnotation());
				}
				JAnnotationUse parameterAnnotation = _class.annotate(LlrpParameterType.class).param("typeNum", typeNum);

				List<String> properties = new ArrayList<>();
				properties.addAll(fields(_class, parameterAnnotation, d.getFieldOrReserved(), false));
				properties.addAll(parameters(_class, d.getParameterOrChoice(), false));
				addProperties(_class, properties);
			} else if (element instanceof ChoiceDefinition) {
				ChoiceDefinition d = (ChoiceDefinition) element;
				String name = d.getName();

				JDefinedClass _class = interfaceClass(name, false);
				if (!d.getAnnotation().isEmpty()) {
					javadoc(_class.javadoc(), d.getAnnotation());
				}

				choiceParameters(_class, d.getParameter());
			} else if (element instanceof EnumerationDefinition) {
				EnumerationDefinition d = (EnumerationDefinition) element;
				String name = d.getName();
				JDefinedClass _class = enumClass(name, false);
				_class._implements(LlrpEnum.class);
				_class.field(JMod.PRIVATE | JMod.FINAL, JPrimitiveType.INT, "value");
				JMethod constructor = _class.constructor(JMod.PRIVATE);
				JVar valueParam = constructor.param(JPrimitiveType.INT, "value");
				constructor.body().assign(JExpr.refthis("value"), valueParam);
				if (!d.getAnnotation().isEmpty()) {
					javadoc(_class.javadoc(), d.getAnnotation());
				}

				for (EnumerationEntryDefinition entry : d.getEntry()) {
					String entryName = entry.getName();
					BigInteger value = entry.getValue();

					JEnumConstant enumConstant = _class.enumConstant(entryName).arg(JExpr.lit(value.intValue()));
					if (!entry.getAnnotation().isEmpty()) {
						javadoc(enumConstant.javadoc(), entry.getAnnotation());
					}
				}

				generateEnumFromValue(_class, d.getEntry());
			} else if (element instanceof CustomMessageDefinition) {
				CustomMessageDefinition d = (CustomMessageDefinition) element;
				String name = d.getName();
				int subType = d.getSubtype();
				String vendor = d.getVendor();
				Long vendorID = vendors.get(vendor);
				if (vendorID == null) {
					throw new IllegalArgumentException("Vendor definition '" + vendor + "' is missing.");
				}

				JDefinedClass _class = messageClass(name, true);
				_class._extends(LlrpMessage.class);
				JAnnotationUse msgAnnotation = _class.annotate(LlrpCustomMessageType.class)
						.param("vendor", vendorID.longValue()).param("subType", subType);
				if (d.getResponseType() != null) {
					msgAnnotation.param("responseType", messageClass(d.getResponseType(), true));
				}
				if (!d.getAnnotation().isEmpty()) {
					javadoc(_class.javadoc(), d.getAnnotation());
				}

				List<String> properties = new ArrayList<>();
				properties.addAll(fields(_class, msgAnnotation, d.getFieldOrReserved(), true));
				properties.addAll(parameters(_class, d.getParameterOrChoice(), true));
				addProperties(_class, properties);

				String responseType = d.getResponseType();
			} else if (element instanceof CustomParameterDefinition) {
				CustomParameterDefinition d = (CustomParameterDefinition) element;

				String name = d.getName();
				long subType = d.getSubtype();
				String vendor = d.getVendor();
				Long vendorID = vendors.get(vendor);
				if (vendorID == null) {
					throw new IllegalArgumentException("Vendor definition '" + vendor + "' is missing.");
				}
				String namespace = d.getNamespace();

				JDefinedClass _class = parameterClass(name, true);
				// extend custom class
				_class._extends(parameterClassRef("Custom", false));
				if (!d.getAnnotation().isEmpty()) {
					javadoc(_class.javadoc(), d.getAnnotation());
				}
				JAnnotationUse parameterAnnotation = _class.annotate(LlrpCustomParameterType.class)
						.param("vendor", vendorID.longValue()).param("subType", subType);

				List<String> properties = new ArrayList<>();
				properties.addAll(fields(_class, parameterAnnotation, d.getFieldOrReserved(), true));
				properties.addAll(parameters(_class, d.getParameterOrChoice(), true));
				addProperties(_class, properties);

				if (!d.getAllowedIn().isEmpty()) {
					allowedInRefs.add(new AllowedInInfo(_class, parameterAnnotation, d.getAllowedIn()));
				}
			} else if (element instanceof CustomChoiceDefinition) {
				CustomChoiceDefinition d = (CustomChoiceDefinition) element;
				String name = d.getName();
				String namespace = d.getNamespace();

				JDefinedClass _class = interfaceClass(name, true);
				if (!d.getAnnotation().isEmpty()) {
					javadoc(_class.javadoc(), d.getAnnotation());
				}

				choiceParameters(_class, d.getParameter());
			} else if (element instanceof CustomEnumerationDefinition) {
				CustomEnumerationDefinition d = (CustomEnumerationDefinition) element;
				String name = d.getName();
				String namespace = d.getNamespace();

				JDefinedClass _class = enumClass(name, true);
				_class._implements(LlrpEnum.class);
				_class.field(JMod.PRIVATE | JMod.FINAL, JPrimitiveType.INT, "value");
				JMethod constructor = _class.constructor(JMod.PRIVATE);
				JVar valueParam = constructor.param(JPrimitiveType.INT, "value");
				constructor.body().assign(JExpr.refthis("value"), valueParam);
				if (!d.getAnnotation().isEmpty()) {
					javadoc(_class.javadoc(), d.getAnnotation());
				}

				for (EnumerationEntryDefinition entry : d.getEntry()) {
					String entryName = entry.getName();
					BigInteger value = entry.getValue();

					JEnumConstant enumConstant = _class.enumConstant(entryName).arg(JExpr.lit(value.intValue()));
					if (!entry.getAnnotation().isEmpty()) {
						javadoc(enumConstant.javadoc(), entry.getAnnotation());
					}
				}
				generateEnumFromValue(_class, d.getEntry());
			} else if (element instanceof Annotation) {
				List<Object> content = ((Annotation) element).getDocumentationOrDescription();
				String source = ((Annotation) element).getSource();
			}
		}
	}

	private void generateEnumFromValue(JDefinedClass _class, List<EnumerationEntryDefinition> entries) {
		JMethod fromValue = _class.method(JMod.STATIC | JMod.PUBLIC, _class, "fromValue");
		JVar valueParam = fromValue.param(JPrimitiveType.INT, "value");
		JSwitch s = fromValue.body()._switch(valueParam);
		for (EnumerationEntryDefinition entry : entries) {
			String entryName = entry.getName();
			BigInteger value = entry.getValue();

			s._case(JExpr.lit(value.intValue())).body()._return(JExpr.enumConstantRef(_class, entryName));
		}
		fromValue.body()._throw(JExpr._new(codeModel._ref(IllegalArgumentException.class)));
	}

	public void generateGettersAndSetters() {
		// put packages into array to avoid concurrent modification exceptions
		List<JPackage> packages = new ArrayList<>();
		for (Iterator<JPackage> it = codeModel.packages(); it.hasNext();) {
			packages.add(it.next());
		}
		// generate getters and setters for fields
		for (JPackage p : packages) {
			for (JDefinedClass c : p.classes()) {
				gettersAndSetters(c);
			}
		}
	}

	public void generateHashCodeAndEquals() {
		// generate hashCode and equals methods
		for (Iterator<JPackage> it = codeModel.packages(); it.hasNext();) {
			JPackage p = it.next();
			for (JDefinedClass c : p.classes()) {
				if (c.isInterface() || c.getClassType() == EClassType.ENUM) {
					continue;
				}
				hashCode(c);
				equals(c);
			}
		}
	}

	static void dropPrefixes(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			node.setPrefix(null);
		}
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			dropPrefixes(list.item(i));
		}
	}

	void javadoc(JDocComment javadoc, List<Annotation> annotations) {
		List<Object> html = new ArrayList<>();
		StringWriter sw = new StringWriter();
		for (Annotation annotation : annotations) {
			for (Object content : annotation.getDocumentationOrDescription()) {
				if (content instanceof Description) {
					html.addAll(((Description) content).getContent());
				}
			}
		}
		for (Annotation annotation : annotations) {
			for (Object content : annotation.getDocumentationOrDescription()) {
				if (content instanceof Documentation) {
					html.addAll(((Documentation) content).getContent());
				}
			}
		}
		if (!html.isEmpty()) {
			for (Object part : html) {
				if (part instanceof Element) {
					dropPrefixes((Element) part);
					try {
						transformer.transform(new DOMSource((Element) part), new StreamResult(sw));
					} catch (TransformerException e) {
						e.printStackTrace();
					}
				} else {
					sw.write(part.toString());
				}
			}
			/* remove xmlns declarations */
			String withoutXmlns = sw.toString().replaceAll("xmlns.*?(\"|\').*?(\"|\')\\s*", "").replaceAll("\\s+>", ">")
					.replaceAll("[ \t]+", " ").replaceAll("\n ", "\n").replaceAll("\n<a", "\n@see <a");
			javadoc.add(withoutXmlns);
		}
	}

	void annotations(List<Annotation> annotations) {
		// this may handle element annotations
	}

	List<String> fields(JDefinedClass _class, JAnnotationUse typeAnnotation, List<Object> fieldOrReserved,
			boolean custom) {
		List<String> fields = new ArrayList<>();

		JAnnotationUse fieldAnnotation = null;
		int reservedBefore = 0;
		for (Object fr : fieldOrReserved) {
			if (fr instanceof FieldDefinition) {
				FieldDefinition fd = (FieldDefinition) fr;
				String enumeration = fd.getEnumeration();
				JFieldVar _field;
				String name = firstLower(fd.getName());
				if (enumeration != null) {
					boolean isArray = fd.getType().name().endsWith("_V");
					AbstractJClass enumType = enumClassRef(enumeration, custom);
					if (isArray) {
						enumType = codeModel.ref(List.class).narrow(enumType);
					}
					_field = _class.field(JMod.PROTECTED, enumType, name);
				} else {
					Class<?> javaType = Types.javaType(fd.getType());
					_field = _class.field(JMod.PROTECTED, javaType, name);
				}
				fieldAnnotation = _field.annotate(LlrpField.class);
				fieldAnnotation.param("type", fd.getType());
				if (fd.getFormat() != null) {
					fieldAnnotation.param("format", fd.getFormat());
				}
				if (reservedBefore > 0) {
					fieldAnnotation.param("reservedBefore", reservedBefore);
					reservedBefore = 0;
				}

				fields.add(name);
			} else if (fr instanceof ReservedDefinition) {
				ReservedDefinition rd = (ReservedDefinition) fr;
				if (fieldAnnotation != null) {
					fieldAnnotation.param("reservedAfter", rd.getBitCount());
				} else {
					reservedBefore = rd.getBitCount();
				}
			}
		}
		if (reservedBefore > 0) {
			typeAnnotation.param("reserved", reservedBefore);
		}
		return fields;
	}

	String firstUpper(String str) {
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	String firstLower(String str) {
		return Character.toLowerCase(str.charAt(0)) + str.substring(1);
	}

	String startLower(String str) {
		StringBuilder sb = new StringBuilder(str.length());
		int length = str.length();
		int i = 0;
		while (i < length && Character.isUpperCase(str.charAt(i))) {
			sb.append(Character.toLowerCase(str.charAt(i)));
			i++;
		}
		if (i > 1 && i < length - 1) {
			sb.replace(i - 1, i, Character.toString(Character.toUpperCase(sb.charAt(i - 1))));
		}
		return sb.append(str.substring(i)).toString();
	}

	void gettersAndSetters(JDefinedClass _class) {
		for (Map.Entry<String, JFieldVar> entry : _class.fields().entrySet()) {
			JFieldVar field = entry.getValue();
			AbstractJType type = field.type();

			if ((field.mods().getValue() & JMod.FINAL) == 0) {
				JMethod setter = _class.method(JMod.PUBLIC, _class, startLower(firstUpper(field.name())));
				JVar value = setter.param(type, field.name());
				setter.body().assign(JExpr.refthis(field), value);
				setter.body()._return(JExpr._this());
			}

			// add a builder method that creates an instance if it not already
			// exists
			boolean isList = type.isReference() && codeModel._ref(List.class).isAssignableFrom(type);
			boolean createIfNull = type.isReference() && (isList || !((AbstractJClass) type).isInterface())
					&& !type.isArray() && !codeModel._ref(Number.class).isAssignableFrom(type)
					&& !type.fullName().contains("enumeration");

			JMethod builder = _class.method(JMod.PUBLIC, type, startLower(firstUpper(field.name())));

			if (createIfNull) {
				JInvocation newInstance;
				if (isList) {
					List<? extends AbstractJClass> typeParams = ((AbstractJClass) type).getTypeParameters();
					AbstractJClass listType = codeModel.ref(ArrayList.class);
					if (!typeParams.isEmpty()) {
						listType = listType.narrow(typeParams.get(0));
					}
					newInstance = JExpr._new(listType);
				} else {
					newInstance = JExpr._new(type);
				}
				builder.body()._if(JExpr.ref(field).eqNull())._then().assign(JExpr.ref(field), newInstance);

				// create dedicated getter to access raw value
				JMethod getter = _class.method(JMod.PUBLIC, type, "get" + firstUpper(field.name()));
				getter.body()._return(JExpr.ref(field));
			}
			builder.body()._return(JExpr.ref(field));
		}
	}

	void equals(JDefinedClass _class) {
		JMethod equals = _class.method(JMod.PUBLIC, JPrimitiveType.BOOLEAN, "equals");
		JVar obj = equals.param(codeModel.ref(Object.class), "obj");
		JBlock body = equals.body();
		body._if(obj.eqNull())._then()._return(JExpr.lit(false));
		body._if(obj.invoke("getClass").ne(JExpr.invoke("getClass")))._then()._return(JExpr.lit(false));

		if (!_class.fields().isEmpty()) {
			JVar o = body.decl(_class, "other");
			body.assign(o, JExpr.cast(_class, obj));

			for (Map.Entry<String, JFieldVar> entry : _class.fields().entrySet()) {
				JFieldVar field = entry.getValue();
				JInvocation eq = codeModel.ref(Objects.class).staticInvoke("equals");
				eq.arg(JExpr.refthis(field)).arg(o.ref(field));
				JBlock then = body._if(eq.not())._then();
				// then.directStatement("System.out.println(\"" +
				// field.owner().name() + "." + field.name() + "\");");
				then._return(JExpr.lit(false));
			}
		}
		body._return(JExpr.lit(true));
	}

	void hashCode(JDefinedClass _class) {
		JMethod hashCode = _class.method(JMod.PUBLIC, JPrimitiveType.INT, "hashCode");
		JInvocation hash = codeModel.ref(Objects.class).staticInvoke("hash");
		hashCode.body()._return(hash);
		for (Map.Entry<String, JFieldVar> entry : _class.fields().entrySet()) {
			JFieldVar field = entry.getValue();
			hash.arg(JExpr.refthis(field));
		}
	}

	List<String> parameters(JDefinedClass _class, List<Object> parameterOrChoice, boolean custom) {
		if (parameterOrChoice.isEmpty()) {
			return Collections.emptyList();
		}

		List<String> parameters = new ArrayList<>();
		for (Object pc : parameterOrChoice) {
			String name, type, repeatExpr;
			AbstractJClass typeClass;
			List<Annotation> annotations;

			if (pc instanceof ParameterReference) {
				ParameterReference pr = (ParameterReference) pc;
				repeatExpr = pr.getRepeat();
				name = pr.getName();
				type = pr.getType();
				typeClass = parameterClassRef(type, custom);
				annotations = pr.getAnnotation();
			} else { // if (pc instanceof ChoiceReference) {
				ChoiceReference cr = (ChoiceReference) pc;
				repeatExpr = cr.getRepeat();
				name = cr.getName();
				type = cr.getType();
				typeClass = interfaceClassRef(type, custom);
				annotations = cr.getAnnotation();
			}
			if (name == null || name.trim().length() == 0) {
				name = firstLower(type);
			}
			Repeat repeat = Repeat.parse(repeatExpr);
			JFieldVar field = parameter(_class, repeat, typeClass, name);
			if (!annotations.isEmpty()) {
				javadoc(field.javadoc(), annotations);
			}
			parameters.add(name);
		}
		return parameters;
	}

	JFieldVar parameter(JDefinedClass _class, Repeat repeat, AbstractJClass typeClass, String name) {
		boolean multiple = repeat == Repeat.R0_TO_N || repeat == Repeat.R1_TO_N;
		AbstractJType fieldType = multiple ? codeModel.ref(List.class).narrow(typeClass) : typeClass;
		JFieldVar field = _class.field(JMod.PROTECTED, fieldType, name);
		field.annotate(LlrpParam.class).param("required", repeat == Repeat.R1 || repeat == Repeat.R1_TO_N);
		return field;
	}

	void choiceParameters(JDefinedClass choiceInterface, List<ChoiceParameterReference> parameters) {
		for (ChoiceParameterReference ref : parameters) {
			String type = ref.getType();
			parameterClass(type, false)._implements(choiceInterface);
		}
	}
}