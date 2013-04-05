package com.test.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.test.model.Company;

/**
 * Service bean for Company entities.
 * <p>
 * This class provides CRUD functionality for all Company entities. It focuses
 * purely on Java EE 6 standards (e.g. <tt>&#64;ConversationScoped</tt> for
 * state management, <tt>PersistenceContext</tt> for persistence,
 * <tt>CriteriaBuilder</tt> for searches) rather than introducing a CRUD
 * framework or custom base class.
 */

@Path("/company")
@Stateful
@RequestScoped
public class CompanyService {

	@PersistenceContext
	private EntityManager entityManager;

	@Inject
	private Validator validator;

	/*
	 * Support retrieving Company entities
	 */

	@GET
	@Path("/{id:[0-9][0-9]*}")
	@Produces(MediaType.APPLICATION_JSON)
	public Company retrieve(@PathParam("id") long id) {

		return this.entityManager.find(Company.class, id);
	}

	/*
	 * Support creating, updating and deleting Company entities
	 */

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response create(Company company) {
		
		return update(null, company);
	}

	@PUT
	@Path("/{id:[0-9][0-9]*}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response update(@PathParam("id") Long id, Company company) {

		company.setId(id);
		
		// Validate

		Set<ConstraintViolation<Company>> violations = this.validator.validate(company);

		if (!violations.isEmpty()) {

			Map<String, String> responseObj = new HashMap<String, String>();

			for (ConstraintViolation<?> violation : violations) {
				responseObj.put(violation.getPropertyPath().toString(), violation.getMessage());
			}

			return Response.status(Response.Status.PRECONDITION_FAILED).entity(responseObj).build();
		}

		// Save
		
		if (id == null) {
			this.entityManager.persist(company);
		} else {
			this.entityManager.merge(company);
		}
		return Response.ok().build();
	}

	@DELETE
	@Path("/{id:[0-9][0-9]*}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") long id) {

		this.entityManager.remove(retrieve(id));
		return Response.ok().build();
	}
	
	/*
	 * Support searching Company entities
	 */
	 
	@POST
	@Path("/search")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public List<Company> getPageItems(Company search) {
		
		CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();		
		CriteriaQuery<Company> criteria = builder.createQuery(Company.class);
		Root<Company> root = criteria.from(Company.class);
		criteria = criteria.select(root);
		
		if ( search != null ) {
			criteria = criteria.where(getSearchPredicates(root, search));
		}
		
		return this.entityManager.createQuery(criteria).getResultList();
	}

	private Predicate[] getSearchPredicates(Root<Company> root, Company search) {

		CriteriaBuilder builder = this.entityManager.getCriteriaBuilder();
		List<Predicate> predicatesList = new ArrayList<Predicate>();

		String name = search.getName();
		if (name != null && !"".equals(name)) {
			predicatesList.add(builder.like(root.<String>get("name"), '%' + name + '%'));
		}
		boolean publiclyListed = search.getPubliclyListed();
		if (publiclyListed) {
			predicatesList.add(builder.equal(root.get("publiclyListed"),publiclyListed));
		}

		return predicatesList.toArray(new Predicate[predicatesList.size()]);
	}

	/*
	 * Support listing and POSTing back Company entities (e.g. from inside a
	 * &lt;select&gt;)
	 */

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Company> getAll() {
		
		return getPageItems( null );
	}
}