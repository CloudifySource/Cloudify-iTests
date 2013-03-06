package org.cloudifysource.quality.iTests.framework.utils.rest;

import org.cloudifysource.dsl.rest.response.DeleteServiceInstanceAttributeResponse;
import org.cloudifysource.dsl.rest.response.Response;
import org.cloudifysource.dsl.rest.response.ServiceDetails;
import org.cloudifysource.dsl.rest.response.ServiceInstanceDetails;
import org.codehaus.jackson.type.TypeReference;

public class ResponseTypeReferenceFactory {

	public static TypeReference<Response<ServiceDetails>> newServiceDetailsResponse() {
		return new TypeReference<Response<ServiceDetails>>() {};
	}
	
	public static TypeReference<Response<Void>> newVoidResponse() {
		return new TypeReference<Response<Void>>() {};
	}
	
	public static TypeReference<Response<DeleteServiceInstanceAttributeResponse>> newDeleteServiceInstanceAttributeResponse() {
		return new TypeReference<Response<DeleteServiceInstanceAttributeResponse>>() {};
	}
	
	public static TypeReference<Response<ServiceInstanceDetails>> newServiceInstanceDetailsResponse() {
		return new TypeReference<Response<ServiceInstanceDetails>>() {};
	}

}
