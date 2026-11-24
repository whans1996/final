package com.vibenote.analysis.structure.xml;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.structure.EdgeType;
import com.vibenote.analysis.structure.StructureLinks;
import com.vibenote.analysis.structure.UnresolvedKind;
import com.vibenote.analysis.structure.UnresolvedReason;
import com.vibenote.analysis.structure.java.JavaTypeIndex;
import java.io.IOException;
import java.io.StringReader;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * MyBatis mapper XML ↔ 선별된 Java 파일 (F-21a AC4).
 *
 * <p>{@code <mapper namespace>}가 선별된 Java FQN이면 Java → XML(줄 1), 타입 속성 값이 선별된 Java FQN이면 XML →
 * Java(그 요소 줄). 요소 줄 번호가 필요해 DOM 대신 SAX로 읽는다. 외부 DTD·외부 엔티티·XInclude는 읽지 않는다 (MyBatis XML은 공개 DTD를
 * 선언하므로 DOCTYPE 자체는 허용한다).
 */
public final class MyBatisXmlReader {

    static final int MAX_DETAIL = 200;

    private static final List<String> TYPE_ATTRIBUTES =
            List.of("resultType", "parameterType", "type", "javaType", "ofType");

    private MyBatisXmlReader() {}

    public static void read(List<AnalysisFile> xmlFiles, JavaTypeIndex index, StructureLinks out) {
        xmlFiles.stream()
                .sorted(Comparator.comparing(AnalysisFile::getId))
                .forEach(file -> read(file, index, out));
    }

    private static void read(AnalysisFile file, JavaTypeIndex index, StructureLinks out) {
        long self = file.getId();
        String code = file.getContent() == null ? "" : file.getContent();
        Handler handler = new Handler(index, code);
        try {
            parser().parse(new InputSource(new StringReader(code)), handler);
        } catch (SAXParseException e) {
            parseError(out, self, Math.max(e.getLineNumber(), 1), e.getMessage());
            return;
        } catch (SAXException | IOException e) {
            parseError(out, self, 1, e.getMessage());
            return;
        }
        if (handler.namespaceFile != null) {
            out.edge(handler.namespaceFile, self, EdgeType.MYBATIS, 1);
        } else if (handler.missingNamespace != null) {
            out.unresolved(
                    self,
                    handler.namespaceLine,
                    UnresolvedKind.IMPORT,
                    UnresolvedReason.NOT_SELECTED,
                    handler.missingNamespace);
        }
        handler.typeLines.forEach((to, line) -> out.edge(self, to, EdgeType.MYBATIS, line));
    }

    private static void parseError(StructureLinks out, long fileId, int line, String message) {
        String detail = message == null || message.isBlank() ? "XML을 읽지 못했습니다." : message.strip();
        out.unresolved(
                fileId,
                line,
                UnresolvedKind.PARSE,
                UnresolvedReason.PARSE_ERROR,
                detail.length() <= MAX_DETAIL ? detail : detail.substring(0, MAX_DETAIL));
    }

    private static SAXParser parser() throws SAXException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            factory.setXIncludeAware(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            SAXParser parser = factory.newSAXParser();
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return parser;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("XML 파서를 설정하지 못했습니다.", e);
        }
    }

    private static final class Handler extends DefaultHandler {

        private final JavaTypeIndex index;
        private final String code;
        private Locator locator;
        Long namespaceFile;
        String missingNamespace;
        int namespaceLine;
        final Map<Long, Integer> typeLines = new TreeMap<>();

        Handler(JavaTypeIndex index, String code) {
            this.index = index;
            this.code = code;
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        /** 외부 엔티티 해석 요청이 와도 빈 내용으로 둔다. */
        @Override
        public InputSource resolveEntity(String publicId, String systemId) {
            return new InputSource(new StringReader(""));
        }

        /** 요소 시작 태그의 첫 줄. SAX Locator는 시작 태그가 끝난 위치를 주므로 거기서 {@code <이름}을 거슬러 찾는다. */
        private int startLine(String qName) {
            if (locator == null || locator.getLineNumber() < 1) {
                return 1;
            }
            int line = locator.getLineNumber();
            int offset = 0;
            for (int i = 1; i < line; i++) {
                offset = code.indexOf('\n', offset) + 1;
                if (offset == 0) {
                    return line;
                }
            }
            int end = Math.min(code.length(), offset + Math.max(locator.getColumnNumber() - 1, 0));
            int start = code.lastIndexOf("<" + qName, end);
            return start < 0
                    ? line
                    : (int) code.substring(0, start).chars().filter(c -> c == '\n').count() + 1;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            int line = startLine(qName);
            if (qName.equals("mapper") && attrs.getValue("namespace") != null) {
                String namespace = attrs.getValue("namespace").strip();
                Optional<Long> target = index.fileOf(namespace);
                if (target.isPresent()) {
                    namespaceFile = target.get();
                } else if (index.inProject(namespace)) {
                    missingNamespace = namespace;
                    namespaceLine = line;
                }
            }
            for (String name : TYPE_ATTRIBUTES) {
                String value = attrs.getValue(name);
                if (value != null) {
                    index.fileOf(value.strip())
                            .ifPresent(to -> typeLines.merge(to, line, Math::min));
                }
            }
        }
    }
}
