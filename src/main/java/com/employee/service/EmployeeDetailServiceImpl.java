package com.employee.service;

import com.employee.dto.EmployeeDetailDto;
import com.employee.model.Employee;
import com.employee.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing MaintenanceService
 *
 * @author Shehan
 */
@Service
@Transactional
public class EmployeeDetailServiceImpl implements EmployeeDetailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmployeeDetailServiceImpl.class);

    @Value("${file.location.input}")
    private String inputFileLocation;

    @Value("${file.location.processed}")
    private String processedFileLocation;

    @Value("${file.location.error}")
    private String errorFileLocation;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public List<EmployeeDetailDto> getAllEmployeeDetails() {
        List<EmployeeDetailDto> employeeDetails = new ArrayList<>();
        employeeRepository.findAll().forEach(i -> employeeDetails.add(new EmployeeDetailDto(i.getName(), i.getSalary())));
        LOGGER.debug("Returning {} of employee details", employeeDetails.size());
        return employeeDetails;
    }

    @Override
    public List<EmployeeDetailDto> getEmployeeDetailsBySalary(String salaryFrom, String salaryTo) {
        //TODO: This should be done via a batch job
        processEmployeeDetailsFromCSVs();

        List<Employee> employeesBySalary = employeeRepository.findEmployeesBySalary(Double.valueOf(salaryFrom), Double.valueOf(salaryTo));
        LOGGER.debug("Returning {} of employee details", employeesBySalary.size());
        return employeesBySalary.stream().map(i -> new EmployeeDetailDto(i.getName(), i.getSalary())).collect(Collectors.toList());
    }

    private void processEmployeeDetailsFromCSVs() {
        LOGGER.debug("Reading all the file from input folder {}", inputFileLocation);
        File folder = new File(inputFileLocation);
        // processContentFromAllFiles(inputFileLocation + "\\EmployeeDetails.csv");
        processContentFromAllFiles(folder);
    }

    private void processContentFromAllFiles(File folder) {
        File[] fileNames = folder.listFiles();
        if (fileNames == null) {
            LOGGER.debug("Files not found in input folder");
        } else {
            LOGGER.debug("{} of files found in input folder", fileNames.length);
            for (File file : fileNames) {
                if (file.isFile()) {
                    try {
                        List<Employee> newEmployeeList = processCSVContent(file);
                        LOGGER.info("{} of new employees has been added.", newEmployeeList.size());
                        moveToProcessedFileLocation(file, inputFileLocation, processedFileLocation);
                    } catch (IOException e) {
                        LOGGER.error("Error occurred while reading from files", e);
                        throw new RuntimeException(e);
                    }
                } else {
                    LOGGER.debug("Invalid file or folder");
                }
            }
        }
    }

    private List<Employee> processCSVContent(File file) throws IOException {
        List<Employee> employeeDetails = new ArrayList<>();
        LOGGER.debug("read file {}", file.getCanonicalPath());
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String strLine;
            // Keeps track of count to avoid header
            int lineCount = 0;
            // Read lines from the file, returns null when end of stream is reached
            while ((strLine = br.readLine()) != null) {
                if (lineCount != 0) {
                    LOGGER.debug("Line is - {}", strLine);
                    String[] split = strLine.split(",");
                    Employee newEmployee = new Employee(split[0], Double.parseDouble(split[1]));
                    employeeDetails.add(newEmployee);
                    saveEmployee(newEmployee);
                }
                lineCount++;
            }
            LOGGER.debug("{} of lines processed", lineCount);
        }

        return employeeDetails;
    }

    private void moveToProcessedFileLocation(File file, String source, String destination) throws IOException {
        Path sourcePath = Paths.get(source + "\\" + file.getName());
        Path destinationPath = Paths.get(destination + "\\" + file.getName());
        Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        LOGGER.debug("File moved to processed folder {}", processedFileLocation);
    }

    private void saveEmployee(Employee employee) {
        LOGGER.info("Saving a new employee {}", employee);
        if (employee != null) {
            employeeRepository.save(employee);
        }
    }

    private void processContentFromAllFiles(String path) {
        Path filePath = Paths.get(path);
        if (Files.isRegularFile(filePath)) {
            try {
                processCSVContent(filePath);
            } catch (IOException e) {
                LOGGER.error("Error occurred while reading from files", e);
                throw new RuntimeException(e);
            }
        }

    }

    private void processCSVContent(Path filePath) throws IOException {
        List<String> fileList = Files.readAllLines(filePath);
        LOGGER.debug("" + fileList);
    }


}