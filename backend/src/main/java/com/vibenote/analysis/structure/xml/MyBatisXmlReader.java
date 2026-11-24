package com.vibenote.analysis.structure.xml;

import com.vibenote.analysis.domain.AnalysisFile;
import com.vibenote.analysis.structure.EdgeType;
import com.vibenote.analysis.structure.StructureLinks;
import com.vibenote.analysis.structure.UnresolvedKind;
import com.vibenote.analysis.structure.UnresolvedReason;
import com.vibenote.analysis.structure.java.JavaTypeIndex;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
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

    /** 내부 엔티티를 펼친 전체 크기 상한. MyBatis XML은 내부 엔티티를 쓰지 않으므로 작게 둔다. */
    static final String MAX_ENTITY_SIZE = "100000";

    /** 시작 태그 {@code <이름}을 거슬러 찾는 범위. 이보다 긴 시작 태그(엔티티로 만든 요소 포함)는 끝난 줄을 쓴다. */
    private static final int MAX_TAG_LENGTH = 8_192;

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
            parser.setProperty(
                    "http://www.oracle.com/xml/jaxp/properties/totalEntitySizeLimit",
                    MAX_ENTITY_SIZE);
            parser.setProperty(
                    "http://www.oracle.com/xml/jaxp/properties/maxGeneralEntitySizeLimit",
                    MAX_ENTITY_SIZE);
            return parser;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("XML 파서를 설정하지 못했습니다.", e);
        }
    }

    private static final class Handler extends DefaultHandler {

        private final JavaTypeIndex index;
        private final String code;

        /** 줄 시작 offset (0부터). 요소마다 처음부터 줄을 다시 세지 않게 한 번 만든다. */
        private final int[] lineStarts;

        private Locator locator;
        Long namespaceFile;
        String missingNamespace;
        int namespaceLine;
        final Map<Long, Integer> typeLines = new TreeMap<>();

        Handler(JavaTypeIndex index, String code) {
            this.index = index;
            this.code = code;
            this.lineStarts = lineStarts(code);
        }

        private static int[] lineStarts(String code) {
            int count = 1;
            for (int i = 0; i < code.length(); i++) {
                if (code.charAt(i) == '\n') {
                    count++;
                }
            }
            int[] starts = new int[count];
            int k = 1;
            for (int i = 0; i < code.length(); i++) {
                if (code.charAt(i) == '\n') {
                    starts[k++] = i + 1;
                }
            }
            return starts;
        }

        /** offset이 있는 줄 (1부터). */
        private int lineOf(int offset) {
            int found = Arrays.binarySearch(lineStarts, offset);
            return found >= 0 ? found + 1 : -found - 1;
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
            if (line > lineStarts.length) {
                return line;
            }
            int offset = lineStarts[line - 1];
            int end = Math.min(code.length(), offset + Math.max(locator.getColumnNumber() - 1, 0));
            String open = "<" + qName;
            int floor = Math.max(0, end - MAX_TAG_LENGTH);
            for (int start = Math.min(end, code.length() - open.length());
                    start >= floor;
                    start--) {
                if (code.startsWith(open, start)) {
                    return lineOf(start);
                }
            }
            return line;
        }

        /** MyBatis는 중첩 타입을 이진 이름({@code Dto$Row})으로 쓴다. 색인은 정식 이름({@code Dto.Row})이다. */
        private static String canonical(String name) {
            return name.strip().replace('$', '.');
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            int line = startLine(qName);
            if (qName.equals("mapper") && attrs.getValue("namespace") != null) {
                String namespace = canonical(attrs.getValue("namespace"));
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
                    index.fileOf(canonical(value))
                            .ifPresent(to -> typeLines.merge(to, line, Math::min));
                }
            }
        }
    }
}
