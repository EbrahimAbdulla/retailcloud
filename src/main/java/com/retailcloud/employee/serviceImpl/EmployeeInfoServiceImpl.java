package com.retailcloud.employee.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.reflect.TypeToken;
import com.retailcloud.employee.dto.EmployeDto;
import com.retailcloud.employee.entity.AddressInfo;
import com.retailcloud.employee.entity.DepartmentInfo;
import com.retailcloud.employee.entity.EmployeeInfo;
import com.retailcloud.employee.model.DepartmentInfoModel;
import com.retailcloud.employee.model.DepartmentInformationResponseModel;
import com.retailcloud.employee.model.EmployeeInfoModel;
import com.retailcloud.employee.model.EmployeeInformationResponseModel;
import com.retailcloud.employee.model.PageMetaModel;
import com.retailcloud.employee.model.ResponseModel;
import com.retailcloud.employee.model.SummaryResponseModel;
import com.retailcloud.employee.repository.DepartmentInfoRepository;
import com.retailcloud.employee.repository.EmployeeInfoRepository;
import com.retailcloud.employee.service.EmployeeInfoService;
import com.retailcloud.employee.utiil.Utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.validation.Valid;
@Service
public class EmployeeInfoServiceImpl implements EmployeeInfoService{
	
	
	 /**
     * The ModelMapper is automatically injected to facilitate object-to-object
     * mapping and conversion.
     */

    @Autowired
    ModelMapper mapper;
    
    @Autowired
    EmployeeInfoRepository employeeRepo;
    
    @Autowired
    Utils utils;
    @Autowired
    DepartmentInfoRepository departmentRepo;
    
	@Autowired
	EntityManager entityManager;
    
    

	@Override
	public ResponseModel saveOrUpdateEmployeeInfo(EmployeeInfoModel employeeModel) {
		try {
			mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
			EmployeeInfo employeeObj = mapper.map(employeeModel, EmployeeInfo.class);
			EmployeeInfo data = null;
			if(employeeModel.getReportingManagerId() != null) {
				Optional<EmployeeInfo> reportingManger = employeeRepo.findById(employeeModel.getReportingManagerId());
				if(reportingManger.isPresent() && Boolean.TRUE.equals(reportingManger.get().getIsReportingManager())) {	
					employeeObj.setReportingManagerDetails(reportingManger.get());
				}else {
			        return new ResponseModel(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT, null, "reporting manager not found with this id "+employeeModel.getReportingManagerId());		
				}
			}else {
		        return new ResponseModel(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT, null, "without reporting manger employee details cannot be save.");		
			}
			if (employeeModel.getEmployeeId() != null) {
				Optional<EmployeeInfo> employeClass = employeeRepo.findById(employeeModel.getEmployeeId());
				if(employeClass.isPresent()) {
					EmployeeInfo employeeInfo = employeClass.get();
					utils.updateEmploeeInfo(employeeInfo, employeeModel);
					data = employeeRepo.save(employeeInfo);
				}else {
			        return new ResponseModel(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT, null, "Employee is not present.");		

				}
			} else {
				data = employeeRepo.save(employeeObj);
			}
			EmployeeInfoModel employeeResponse = mapper.map(data, EmployeeInfoModel.class);
	        return new ResponseModel(HttpStatus.CREATED.value(), HttpStatus.CREATED, employeeResponse, "Successfully");
		} catch (Exception e) {
			e.printStackTrace();
	        return new ResponseModel(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT, null, "Exception occured while saving employee details.");		
		}
	}

	@Override
	public SummaryResponseModel fetchAllEmployeeDetails(@Valid EmployeDto dto) {
		List<EmployeeInfoModel> responseList = new ArrayList<>();

		List<EmployeeInfo> employeeDetails = employeeRepo.findAll();
		
		if(CollectionUtils.isEmpty(employeeDetails)) {
			return new SummaryResponseModel(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT, null,
	                "Employee Details not found.", null);
		}
		
		for(EmployeeInfo employee:employeeDetails) {
			EmployeeInfoModel employe = mapper.map(employee, EmployeeInfoModel.class);
			if(employee.getReportingManagerDetails() != null) {
				EmployeeInfoModel reportingManager = mapper.map(employee.getReportingManagerDetails(), EmployeeInfoModel.class);
				employe.setReportingManagerDetails(reportingManager);
				responseList.add(employe);
			}else {
				responseList.add(employe);
			}		
		}
		PageMetaModel metaModel = null;
		 if(dto.getPage() != null && dto.getSize()!=  null) {
         	Long totalElements = (long) responseList.size();
 			int totalPages = (int) Math.ceil((double) totalElements / dto.getSize());
 			metaModel = new PageMetaModel(dto.getPage(), dto.getSize(), totalElements, totalPages);
 			int firstIndex = (dto.getPage() - 1) * dto.getSize();
 			int maxIndex = dto.getSize() + firstIndex;
 			List<EmployeeInfoModel> paginatedData = null;

 			if (responseList != null && responseList.size() <= maxIndex) {
 				firstIndex = Math.min((dto.getPage() - 1) * dto.getSize(), responseList.size());
 				maxIndex = Math.min(dto.getSize() + firstIndex, responseList.size());
 			}

 			paginatedData = responseList.subList(firstIndex, maxIndex);
 			return new SummaryResponseModel(HttpStatus.OK.value(), HttpStatus.OK, paginatedData,
 					"Data fetched successfully", metaModel); 
		 }
		
		return new SummaryResponseModel(HttpStatus.OK.value(), HttpStatus.OK, responseList,
                "Fetched details successfully.", null);	
	}
}