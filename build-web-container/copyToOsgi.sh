# copies the results of a web container build (ant clean collect) to the relevant directories of the given OSGi trunk
REPO=$1/licensed/repo
SRC=target/bundles
cp $SRC/org.eclipse.virgo.util.osgi-3.0.0.*.jar $REPO/org.eclipse.virgo.util.osgi/org.eclipse.virgo.util.osgi-3.0.0.jar
cp $SRC/org.eclipse.virgo.util.common-3.0.0.*.jar $REPO/org.eclipse.virgo.util.common/org.eclipse.virgo.util.common-3.0.0.jar
cp $SRC/org.eclipse.virgo.util.io-3.0.0.*.jar $REPO/org.eclipse.virgo.util.io/org.eclipse.virgo.util.io-3.0.0.jar
cp $SRC/org.eclipse.virgo.util.math-3.0.0.*.jar $REPO/org.eclipse.virgo.util.math/org.eclipse.virgo.util.math-3.0.0.jar
cp $SRC/org.eclipse.virgo.util.parser.manifest-3.0.0.*.jar $REPO/org.eclipse.virgo.util.parser.manifest/org.eclipse.virgo.util.parser.manifest-3.0.0.jar
cp $SRC/org.springframework.aop-2.5.6.*.jar $REPO/org.springframework.aop/org.springframework.aop-2.5.6.jar
cp $SRC/org.springframework.beans-2.5.6.*.jar $REPO/org.springframework.beans/org.springframework.beans-2.5.6.jar
cp $SRC/org.springframework.context-2.5.6.*.jar $REPO/org.springframework.context/org.springframework.context-2.5.6.jar
cp $SRC/org.springframework.core-2.5.6.*.jar $REPO/org.springframework.core/org.springframework.core-2.5.6.jar
cp $SRC/org.springframework.osgi.core-1.2.1.jar $REPO/org.springframework.osgi.core/org.springframework.osgi.core-1.2.1.jar
cp $SRC/org.springframework.osgi.io-1.2.1.jar $REPO/org.springframework.osgi.io/org.springframework.osgi.io-1.2.1.jar
cp $SRC/com.springsource.org.apache.catalina.ha-7.0.26.jar $REPO/com.springsource.org.apache.catalina.ha/com.springsource.org.apache.catalina.ha-7.0.26.jar
cp $SRC/com.springsource.org.apache.catalina-7.0.26.jar $REPO/com.springsource.org.apache.catalina/com.springsource.org.apache.catalina-7.0.26.jar
cp $SRC/com.springsource.org.apache.catalina.tribes-7.0.26.jar $REPO/com.springsource.org.apache.catalina.tribes/com.springsource.org.apache.catalina.tribes-7.0.26.jar
cp $SRC/com.springsource.org.apache.coyote-7.0.26.jar $REPO/com.springsource.org.apache.coyote/com.springsource.org.apache.coyote-7.0.26.jar
cp $SRC/com.springsource.org.apache.el-7.0.26.jar $REPO/com.springsource.org.apache.el/com.springsource.org.apache.el-7.0.26.jar
cp $SRC/com.springsource.org.apache.jasper-7.0.26.jar $REPO/com.springsource.org.apache.jasper/com.springsource.org.apache.jasper-7.0.26.jar
cp $SRC/com.springsource.org.apache.juli.extras-7.0.26.jar $REPO/com.springsource.org.apache.juli.extras/com.springsource.org.apache.juli.extras-7.0.26.jar
cp $SRC/com.springsource.org.apache.tomcat.api-7.0.26.jar $REPO/com.springsource.org.apache.tomcat.api/com.springsource.org.apache.tomcat.api-7.0.26.jar
cp $SRC/com.springsource.org.apache.tomcat.util-7.0.26.jar $REPO/com.springsource.org.apache.tomcat.util/com.springsource.org.apache.tomcat.util-7.0.26.jar
cp $SRC/com.springsource.org.apache.commons.logging-1.1.1.jar $REPO/com.springsource.org.apache.commons.logging/com.springsource.org.apache.commons.logging-1.1.1.jar
cp $SRC/org.eclipse.gemini.web.core-2.0.0.*.jar $REPO/org.eclipse.gemini.web.core/org.eclipse.gemini.web.core-2.0.0.jar
cp $SRC/org.eclipse.gemini.web.extender-2.0.0.*.jar $REPO/org.eclipse.gemini.web.extender/org.eclipse.gemini.web.extender-2.0.0.jar
cp $SRC/org.eclipse.gemini.web.tomcat-2.0.0.*.jar $REPO/org.eclipse.gemini.web.tomcat/org.eclipse.gemini.web.tomcat-2.0.0.jar
