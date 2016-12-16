<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" encoding="utf-8" indent="yes"/>

	<xsl:param name="GW.VERSION"/>

	<xsl:template match="/">

		<feature id="org.eclipse.gemini.web.feature" label="%featureName" provider-name="%providerName">

			<xsl:attribute name="version"><xsl:value-of select="$GW.VERSION"/></xsl:attribute>

			<description url="http://eclipse.org/gemini/web">%description</description>

			<copyright url="http://eclipse.org/gemini/web">%copyright</copyright>

			<license url="%licenseURL">%license</license>

			<plugin id="org.eclipse.gemini.web.tomcat" download-size="0" install-size="0" version="0.0.0" unpack="false"/>

			<plugin id="org.eclipse.gemini.web.extender" download-size="0" install-size="0" version="0.0.0" unpack="false"/>

			<plugin id="org.eclipse.gemini.web.core" download-size="0" install-size="0" version="0.0.0" unpack="false"/>

			<plugin id="org.eclipse.gemini.web.jaspic.fragment" download-size="0" install-size="0" version="0.0.0" unpack="false"/>

			<xsl:for-each select="ivy-module/dependencies/dependency">
				<plugin>
					<xsl:attribute name="id"><xsl:value-of select="@name"/></xsl:attribute>
					<xsl:attribute name="download-size">0</xsl:attribute>
					<xsl:attribute name="install-size">0</xsl:attribute>
					<xsl:attribute name="version">0.0.0</xsl:attribute>
					<xsl:attribute name="unpack">false</xsl:attribute>
				</plugin>
			</xsl:for-each>

		</feature>

	</xsl:template>

</xsl:stylesheet>