<?xml version="1.0" encoding="utf-8"?>
<!-- based on SO answer http://stackoverflow.com/a/9471505/166062 by dvdvorle http://stackoverflow.com/users/481635/dvdvorle -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text" indent="no"/>
    <xsl:template match="/testsuite">
        <xsl:text>Testsuite: </xsl:text><xsl:value-of select="@name" />
        <xsl:text>
Tests run: </xsl:text>
        <xsl:value-of select="@tests" />
        <xsl:text>, Failures: </xsl:text>
        <xsl:value-of select="@failures" />
        <xsl:text>, Errors: </xsl:text>
        <xsl:value-of select="@errors" />
        <xsl:text>, Time elapsed: </xsl:text>
        <xsl:value-of select="@time" />
        <xsl:text> sec</xsl:text>
        <xsl:text>
--------- ----------- ---------</xsl:text>
        <xsl:apply-templates select="testcase" />
        <xsl:apply-templates select="system-out" />
        <xsl:apply-templates select="system-err" />
    </xsl:template>

    <xsl:template match="testcase">
    <xsl:text>
Testcase: </xsl:text>
        <xsl:value-of select="@name" />
        <xsl:text> took </xsl:text>
        <xsl:value-of select="@time" />
        <xsl:choose>
            <xsl:when test="failure"><xsl:text> FAILURE </xsl:text><xsl:apply-templates select="failure"/></xsl:when>
            <xsl:when test="error"><xsl:text> ERROR </xsl:text><xsl:apply-templates select="error"/></xsl:when>
            <xsl:otherwise><xsl:text> SUCCESS</xsl:text></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="error | failure">
        <xsl:value-of select="@message" />
        <xsl:if test="@type != @message">
            <xsl:text> </xsl:text><xsl:value-of select="@type" />
        </xsl:if>
        <xsl:text>
</xsl:text>
        <xsl:value-of select="." />
    </xsl:template>

    <xsl:template match="system-out">
    <xsl:text>
------ Standard output ------
</xsl:text>
        <xsl:value-of select="." />
    </xsl:template>

    <xsl:template match="system-err">
    <xsl:text>
------ Error output ------
</xsl:text>
        <xsl:value-of select="." />
    </xsl:template>

</xsl:stylesheet>
