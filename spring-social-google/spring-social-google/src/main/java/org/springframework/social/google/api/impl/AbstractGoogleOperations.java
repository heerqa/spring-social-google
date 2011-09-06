package org.springframework.social.google.api.impl;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_ATOM_XML;
import static org.springframework.social.google.api.impl.helper.EntryExtractor.NamespaceContext;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.social.MissingAuthorizationException;
import org.springframework.social.UncategorizedApiException;
import org.springframework.social.google.api.impl.helper.EntryExtractor;
import org.springframework.web.client.RestTemplate;

public class AbstractGoogleOperations {

	protected final RestTemplate restTemplate;
	private final boolean isAuthorized;

	public AbstractGoogleOperations(RestTemplate restTemplate, boolean isAuthorized) {
		this.restTemplate = restTemplate;
		this.isAuthorized = isAuthorized;
		
	}

	protected void requireAuthorization() {
		if (!isAuthorized) {
			throw new MissingAuthorizationException();
		}
	}
	
	protected <E> E extractEntry(String url, EntryExtractor<E> extractor) {
		Document document = getDocument(url);
		Element entry = document.getRootElement();
		return extractor.extractEntry(entry);
	}
	
	protected <E> List<E> extractFeedEntries(String url, EntryExtractor<E> extractor) {
		Document document = getDocument(url);
		Nodes entries = document.query("/atom:feed/atom:entry", NamespaceContext);
		List<E> list = new ArrayList<E>();
		for(int i = 0; i < entries.size(); i++) {
			Element entry = (Element)entries.get(i);
			list.add(extractor.extractEntry(entry));
		}
		return list;
	}
	
	private String urlDecode(String url) {
		try {
			return URLDecoder.decode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	private Document getDocument(String url) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_ATOM_XML);
		HttpEntity<StreamSource> requestEntity = new HttpEntity<StreamSource>(headers);
		
		ResponseEntity<StreamSource> response = restTemplate.exchange(
				urlDecode(url), GET, requestEntity, StreamSource.class);
		
		InputStream inputStream = response.getBody().getInputStream();
		Builder parser = new Builder();
		Document document;
		try {
			document = parser.build(inputStream);
		} catch(Exception e) {
			throw new UncategorizedApiException("Error parsing response XML", e);
		}
		return document;
	}
	
	protected <E> E postEntry(String url, Element entry, EntryExtractor<E> responseEntryExtractor) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(APPLICATION_ATOM_XML);
		Source requestSource = new StreamSource(toInputStream(entry.toXML()));
		HttpEntity<Source> request = new HttpEntity<Source>(requestSource, headers);
		
		System.out.println(entry.toXML());
		
		ResponseEntity<StreamSource> response = restTemplate.exchange(url, HttpMethod.POST, request, StreamSource.class);
		InputStream responseSource = response.getBody().getInputStream();
		Element responseEntry;
		try {
			responseEntry = new Builder().build(responseSource).getRootElement();
		} catch(Exception e) {
			throw new UncategorizedApiException("Error parsing response XML", e);
		}
		return responseEntryExtractor.extractEntry(responseEntry);
	}
	
}