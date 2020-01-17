/**
 * DSS - Digital Signature Services
 * Copyright (C) 2015 European Commission, provided under the CEF programme
 * 
 * This file is part of the "DSS - Digital Signature Services" project.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package eu.europa.esig.dss.asic.cades.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import eu.europa.esig.dss.diagnostic.CertificateWrapper;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.diagnostic.TimestampWrapper;
import eu.europa.esig.dss.diagnostic.jaxb.XmlDigestMatcher;
import eu.europa.esig.dss.enumerations.ArchiveTimestampType;
import eu.europa.esig.dss.enumerations.CertificateSourceType;
import eu.europa.esig.dss.enumerations.DigestMatcherType;
import eu.europa.esig.dss.enumerations.TimestampType;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.test.signature.PKIFactoryAccess;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;

public class DSS1421Test extends PKIFactoryAccess {
	
	@Test
	public void test() {
		DSSDocument document = new FileDocument("src/test/resources/validation/dss1421.asice");
		SignedDocumentValidator validator = SignedDocumentValidator.fromDocument(document);
		validator.setCertificateVerifier(getOfflineCertificateVerifier());
		
		Reports reports = validator.validateDocument();
		DiagnosticData diagnosticData = reports.getDiagnosticData();
		List<CertificateWrapper> certificatesFromTimestamps = diagnosticData.getCertificatesFromSource(CertificateSourceType.TIMESTAMP);
		assertEquals(5, certificatesFromTimestamps.size());
		
		SignatureWrapper signature = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
		List<TimestampWrapper> timestamps = signature.getTimestampList();
		assertEquals(2, timestamps.size());
		int archiveTimestampCounter = 0;
		for (TimestampWrapper timestamp : timestamps) {
			CertificateWrapper timestampSigningCertificate = timestamp.getSigningCertificate();
			assertNotNull(timestampSigningCertificate);
			assertTrue(Utils.isCollectionNotEmpty(timestamp.getCertificateChain()));
			if (TimestampType.ARCHIVE_TIMESTAMP.equals(timestamp.getType())) {
				assertTrue(certificatesFromTimestamps.contains(timestampSigningCertificate));
				assertEquals(ArchiveTimestampType.CAdES_DETACHED, timestamp.getArchiveTimestampType());
				archiveTimestampCounter++;
			}
		}
		assertEquals(1, archiveTimestampCounter);
	}

	@Test
	public void testBroken() {
		DSSDocument document = new FileDocument("src/test/resources/validation/dss1421-broken.asice");
		SignedDocumentValidator validator = SignedDocumentValidator.fromDocument(document);
		validator.setCertificateVerifier(getOfflineCertificateVerifier());

		Reports reports = validator.validateDocument();
		DiagnosticData diagnosticData = reports.getDiagnosticData();
		List<CertificateWrapper> certificatesFromTimestamps = diagnosticData.getCertificatesFromSource(CertificateSourceType.TIMESTAMP);
		assertEquals(5, certificatesFromTimestamps.size());

		SignatureWrapper signature = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
		List<TimestampWrapper> timestamps = signature.getTimestampList();
		assertEquals(2, timestamps.size());
		int archiveTimestampCounter = 0;
		for (TimestampWrapper timestamp : timestamps) {
			CertificateWrapper timestampSigningCertificate = timestamp.getSigningCertificate();
			assertNotNull(timestampSigningCertificate);
			assertTrue(Utils.isCollectionNotEmpty(timestamp.getCertificateChain()));
			if (TimestampType.ARCHIVE_TIMESTAMP.equals(timestamp.getType())) {
				assertTrue(certificatesFromTimestamps.contains(timestampSigningCertificate));
				assertEquals(ArchiveTimestampType.CAdES_DETACHED, timestamp.getArchiveTimestampType());
				archiveTimestampCounter++;
				
				List<XmlDigestMatcher> digestMatchers = timestamp.getDigestMatchers();
				boolean validMessageImprint = true;
				for (XmlDigestMatcher xmlDigestMatcher : digestMatchers) {
					if (DigestMatcherType.MESSAGE_IMPRINT == xmlDigestMatcher.getType()) {
						validMessageImprint = xmlDigestMatcher.isDataIntact();
					}
				}
				assertFalse(validMessageImprint);
				assertFalse(timestamp.isMessageImprintDataIntact());
			}
			assertTrue(timestamp.isSignatureValid());
		}
		assertEquals(1, archiveTimestampCounter);
	}

	@Test
	public void testArchiveNotCoverSignature() {
		DSSDocument document = new FileDocument("src/test/resources/validation/dss1421-archive-not-cover.asice");
		SignedDocumentValidator validator = SignedDocumentValidator.fromDocument(document);
		validator.setCertificateVerifier(getOfflineCertificateVerifier());

		Reports reports = validator.validateDocument();
		DiagnosticData diagnosticData = reports.getDiagnosticData();
		List<CertificateWrapper> certificatesFromTimestamps = diagnosticData.getCertificatesFromSource(CertificateSourceType.TIMESTAMP);
		assertEquals(5, certificatesFromTimestamps.size());

		SignatureWrapper signature = diagnosticData.getSignatureById(diagnosticData.getFirstSignatureId());
		List<TimestampWrapper> timestamps = signature.getTimestampList();
		assertEquals(1, timestamps.size());
		int archiveTimestampCounter = 0;
		for (TimestampWrapper timestamp : timestamps) {
			CertificateWrapper timestampSigningCertificate = timestamp.getSigningCertificate();
			assertNotNull(timestampSigningCertificate);
			assertTrue(Utils.isCollectionNotEmpty(timestamp.getCertificateChain()));
			if (TimestampType.ARCHIVE_TIMESTAMP.equals(timestamp.getType())) {
				archiveTimestampCounter++;
			}
		}
		assertEquals(0, archiveTimestampCounter);
	}

	@Override
	protected String getSigningAlias() {
		return null;
	}

}
