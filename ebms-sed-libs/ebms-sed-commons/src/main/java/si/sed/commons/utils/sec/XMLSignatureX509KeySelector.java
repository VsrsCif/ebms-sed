/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils.sec;

import java.security.Key;
import java.security.KeyStoreException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import javax.xml.crypto.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.keyinfo.*;

public class XMLSignatureX509KeySelector extends KeySelector {

    public XMLSignatureX509KeySelector() {

    }

    @Override
    public KeySelectorResult select(KeyInfo keyInfo,
            KeySelector.Purpose purpose, AlgorithmMethod method,
            XMLCryptoContext context) throws KeySelectorException {
        //System.out.println("X509KeySelector.select ");
        SignatureMethod sm = (SignatureMethod) method;

        try {
            // return null if keyinfo is null or keystore is empty
            if (keyInfo == null) {
                return new SimpleKeySelectorResult(null);
            }
            //System.out.println("X509KeySelector.select 1");

            // Iterate through KeyInfo types
            Iterator i = keyInfo.getContent().iterator();
            while (i.hasNext()) {
                XMLStructure kiType = (XMLStructure) i.next();
                // check X509Data
                //System.out.println("X509KeySelector.select 2");
                if (kiType instanceof X509Data) {
                    X509Data xd = (X509Data) kiType;
                    KeySelectorResult ksr = x509DataSelect(xd, sm);
                    //System.out.println("X509KeySelector.select 2.1");
                    return ksr;
                    // check RetrievalMethod
                } else if (kiType instanceof RetrievalMethod) {
                    //System.out.println("X509KeySelector.select 3");
                    RetrievalMethod rm = (RetrievalMethod) kiType;
                    try {
                        KeySelectorResult ksr = null;
                        if (rm.getType().equals(X509Data.RAW_X509_CERTIFICATE_TYPE)) {
                            OctetStreamData data = (OctetStreamData) rm.dereference(context);
                            CertificateFactory cf
                                    = CertificateFactory.getInstance("X.509");
                            X509Certificate cert = (X509Certificate) cf.generateCertificate(data.getOctetStream());
                            ksr = certSelect(cert, sm);
                        } else if (rm.getType().equals(X509Data.TYPE)) {
                            //System.out.println("X509KeySelector.select 4");
                            NodeSetData nd = (NodeSetData) rm.dereference(context);
                            // convert nd to X509Data
                            // ksr = x509DataSelect(xd, sm);
                        } else {
                            // skip; keyinfo type is not supported
                            continue;
                        }
                        if (ksr != null) {
                            return ksr;
                        }
                    } catch (Exception e) {
                        throw new KeySelectorException(e);
                    }
                }
            }
        } catch (KeyStoreException kse) {
            // throw exception if keystore is uninitialized
            throw new KeySelectorException(kse);
        }

        // return null since no match could be found
        return new SimpleKeySelectorResult(null);
    }

    /**
     * Searches the specified keystore for a certificate that matches the
     * criteria specified in the CertSelector.
     *
     * @return a KeySelectorResult containing the cert's public key if there is
     * a match; otherwise null
     */
    /**
     * Searches the specified keystore for a certificate that matches the
     * specified X509Certificate and contains a public key that is compatible
     * with the specified SignatureMethod.
     *
     * @return a KeySelectorResult containing the cert's public key if there is
     * a match; otherwise null
     */
    private KeySelectorResult certSelect(X509Certificate xcert,
            SignatureMethod sm) throws KeyStoreException {
        // skip non-signer certs
        //System.out.println("X509KeySelector.certSelect 1");
        System.out.println("Got cert: " + xcert.getSubjectDN().toString());
        boolean[] keyUsage = xcert.getKeyUsage();
        if (keyUsage != null && keyUsage[0] == false) {
            return null;
        }
        //System.out.println("X509KeySelector.certSelect 2: "+xcert.getPublicKey());

        return new SimpleKeySelectorResult(xcert.getPublicKey());
        /*
        String alias = ks.getCertificateAlias(xcert);
        if (alias != null) {
            PublicKey pk = ks.getCertificate(alias).getPublicKey();
            // make sure algorithm is compatible with method
            if (algEquals(sm.getAlgorithm(), pk.getAlgorithm())) {
                return new SimpleKeySelectorResult(pk);
            }
        }
	return null;*/
    }

    /**
     * Returns an OID of a public-key algorithm compatible with the specified
     * signature algorithm URI.
     */
    private String getPKAlgorithmOID(String algURI) {
        if (algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1)) {
            return "1.2.840.10040.4.1";
        } else if (algURI.equalsIgnoreCase(SignatureMethod.RSA_SHA1)) {
            return "1.2.840.113549.1.1";
        } else {
            return null;
        }
    }

    /**
     * A simple KeySelectorResult containing a public key.
     */
    private static class SimpleKeySelectorResult implements KeySelectorResult {

        private final Key key;

        SimpleKeySelectorResult(Key key) {
            this.key = key;
        }

        @Override
        public Key getKey() {
            //System.out.println("SimpleKeySelectorResult.getKey " + key);
            return key;
        }
    }

    /**
     * Checks if a JCA/JCE public key algorithm name is compatible with the
     * specified signature algorithm URI.
     */
    //@@@FIXME: this should also work for key types other than DSA/RSA
    private boolean algEquals(String algURI, String algName) {
        if (algName.equalsIgnoreCase("DSA")
                && algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1)) {
            return true;
        } else if (algName.equalsIgnoreCase("RSA")
                && algURI.equalsIgnoreCase(SignatureMethod.RSA_SHA1)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Searches the specified keystore for a certificate that matches an entry
     * of the specified X509Data and contains a public key that is compatible
     * with the specified SignatureMethod.
     *
     * @return a KeySelectorResult containing the cert's public key if there is
     * a match; otherwise null
     */
    private KeySelectorResult x509DataSelect(X509Data xd, SignatureMethod sm)
            throws KeyStoreException, KeySelectorException {

        // convert signature algorithm to compatible public-key alg OID
        String algOID = getPKAlgorithmOID(sm.getAlgorithm());

        KeySelectorResult ksr = null;
        Iterator xi = xd.getContent().iterator();
        //System.out.println("X509KeySelector.x509DataSelect");
        while (xi.hasNext()) {

            Object o = xi.next();
            //System.out.println("X509KeySelector.x509DataSelect 1");
            // check X509Certificate
            if (o instanceof X509Certificate) {
                X509Certificate xcert = (X509Certificate) o;
                //System.out.println("X509KeySelector.x509DataSelect 2");
                ksr = certSelect(xcert, sm);
                break;
                // check X509IssuerSerial
            } /*else if (o instanceof X509IssuerSerial) {
	        X509IssuerSerial xis = (X509IssuerSerial) o;
	        X509CertSelector xcs = new X509CertSelector();
	        try {
	            xcs.setSubjectPublicKeyAlgID(algOID);
	            xcs.setSerialNumber(xis.getSerialNumber());
		    xcs.setIssuer(new X500Principal
		        (xis.getIssuerName()).getName());
	        } catch (IOException ioe) {
		    throw new KeySelectorException(ioe);
		}
		ksr = keyStoreSelect(xcs);
	    // check X509SubjectName
	    } else if (o instanceof String) {
	        String sn = (String) o;
	        X509CertSelector xcs = new X509CertSelector();
	        try {
	            xcs.setSubjectPublicKeyAlgID(algOID);
		    xcs.setSubject(new X500Principal(sn).getName());
		} catch (IOException ioe) {
		    throw new KeySelectorException(ioe);
		}
		ksr = keyStoreSelect(xcs);
	    // check X509SKI
	    } else if (o instanceof byte[]) {
	        byte[] ski = (byte[]) o;
	        X509CertSelector xcs = new X509CertSelector();
	        try {
	            xcs.setSubjectPublicKeyAlgID(algOID);
		} catch (IOException ioe) {
		    throw new KeySelectorException(ioe);
		}
		// DER-encode ski - required by X509CertSelector
		byte[] encodedSki = new byte[ski.length+2];
		encodedSki[0] = 0x04; // OCTET STRING tag value
		encodedSki[1] = (byte) ski.length; // length
		System.arraycopy(ski, 0, encodedSki, 2, ski.length);
		xcs.setSubjectKeyIdentifier(encodedSki);
		ksr = keyStoreSelect(xcs);
	    // check X509CRL
	    // not supported: should use CertPath API
	    }*/ else {
                // skip all other entries
                continue;
            }

        }
        return ksr;
    }
}
