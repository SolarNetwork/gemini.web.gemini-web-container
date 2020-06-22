# copies the results of a web container build (ant clean collect) to the relevant directories of the given OSGi trunk
REPO=$1/licensed/repo
SRC=target/bundles
cp $SRC/org.eclipse.virgo.util.osgi-3.6.3.*.jar $REPO/org.eclipse.virgo.util.osgi/org.eclipse.virgo.util.osgi-3.6.3.jar
cp $SRC/org.eclipse.virgo.util.osgi.manifest-3.6.3.*.jar $REPO/org.eclipse.virgo.util.osgi/org.eclipse.virgo.util.osgi.manifest-3.6.3.jar
cp $SRC/org.eclipse.virgo.util.common-3.6.3.*.jar $REPO/org.eclipse.virgo.util.common/org.eclipse.virgo.util.common-3.6.3.jar
cp $SRC/org.eclipse.virgo.util.io-3.6.3.*.jar $REPO/org.eclipse.virgo.util.io/org.eclipse.virgo.util.io-3.6.3.jar
cp $SRC/org.eclipse.virgo.util.math-3.6.3.*.jar $REPO/org.eclipse.virgo.util.math/org.eclipse.virgo.util.math-3.6.3.jar
cp $SRC/org.eclipse.virgo.util.parser.manifest-3.6.3.*.jar $REPO/org.eclipse.virgo.util.parser.manifest/org.eclipse.virgo.util.parser.manifest-3.6.3.jar
cp $SRC/org.apache.catalina.ha-8.5.56.jar $REPO/org.apache.catalina.ha/org.apache.catalina.ha-8.5.56.jar
cp $SRC/org.apache.catalina-8.5.56.jar $REPO/org.apache.catalina/org.apache.catalina-8.5.56.jar
cp $SRC/org.apache.catalina.tribes-8.5.56.jar $REPO/org.apache.catalina.tribes/org.apache.catalina.tribes-8.5.56.jar
cp $SRC/org.apache.coyote-8.5.56.jar $REPO/org.apache.coyote/org.apache.coyote-8.5.56.jar
cp $SRC/org.apache.el-8.5.56.jar $REPO/org.apache.el/org.apache.el-8.5.56.jar
cp $SRC/org.apache.jasper-8.5.56.jar $REPO/org.apache.jasper/org.apache.jasper-8.5.56.jar
cp $SRC/org.apache.juli.extras-8.5.56.jar $REPO/org.apache.juli.extras/org.apache.juli.extras-8.5.56.jar
cp $SRC/org.apache.tomcat.api-8.5.56.jar $REPO/org.apache.tomcat.api/org.apache.tomcat.api-8.5.56.jar
cp $SRC/org.apache.tomcat.jni-8.5.56.jar $REPO/org.apache.tomcat.jni/org.apache.tomcat.jni-8.5.56.jar
cp $SRC/org.apache.tomcat.util-8.5.56.jar $REPO/org.apache.tomcat.util/org.apache.tomcat.util-8.5.56.jar
cp $SRC/org.apache.tomcat.util.scan-8.5.56.jar $REPO/org.apache.tomcat.util.scan/org.apache.tomcat.util.scan-8.5.56.jar
cp $SRC/org.apache.tomcat.websocket-8.5.56.jar $REPO/org.apache.tomcat.websocket/org.apache.tomcat.websocket-8.5.56.jar
cp $SRC/org.eclipse.gemini.web.core-3.0.0.*.jar $REPO/org.eclipse.gemini.web.core/org.eclipse.gemini.web.core-3.0.0.jar
cp $SRC/org.eclipse.gemini.web.extender-3.0.0.*.jar $REPO/org.eclipse.gemini.web.extender/org.eclipse.gemini.web.extender-3.0.0.jar
cp $SRC/org.eclipse.gemini.web.tomcat-3.0.0.*.jar $REPO/org.eclipse.gemini.web.tomcat/org.eclipse.gemini.web.tomcat-3.0.0.jar
cp $SRC/org.eclipse.gemini.web.jaspic.fragment-3.0.0.*.jar $REPO/org.eclipse.gemini.web.tomcat/org.eclipse.gemini.web.jaspic.fragment-3.0.0.jar
cp $SRC/org.eclipse.jdt.core.compiler.batch-3.10.0.*.jar $REPO/org.eclipse.jdt.core.compiler.batch/org.eclipse.jdt.core.compiler.batch-3.10.0.jar
cp $SRC/org.slf4j.api-1.7.2.*.jar $REPO/org.slf4j.api/org.slf4j.api-1.7.2.jar
cp $SRC/org.slf4j.nop-1.7.2.*.jar $REPO/org.slf4j.nop/org.slf4j.nop-1.7.2.jar
