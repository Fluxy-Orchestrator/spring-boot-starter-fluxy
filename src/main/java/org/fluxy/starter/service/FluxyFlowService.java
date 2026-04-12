package org.fluxy.starter.service;

import lombok.RequiredArgsConstructor;
import org.fluxy.core.model.StepStatus;
import org.fluxy.spring.persistence.entity.FlowStepEntity;
import org.fluxy.spring.persistence.entity.FluxyFlowEntity;
import org.fluxy.spring.persistence.entity.FluxyStepEntity;
import org.fluxy.spring.persistence.repository.FlowStepRepository;
import org.fluxy.spring.persistence.repository.FluxyFlowRepository;
import org.fluxy.spring.persistence.repository.FluxyStepRepository;
import org.fluxy.starter.dto.AddStepByNameToFlowRequest;
import org.fluxy.starter.dto.AddStepToFlowRequest;
import org.fluxy.starter.dto.CreateFluxyFlowRequest;
import org.fluxy.starter.dto.FlowStepDto;
import org.fluxy.starter.dto.FluxyFlowDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de negocio para gestionar FluxyFlow y los steps que lo componen.
 */
@RequiredArgsConstructor
public class FluxyFlowService {

    private final FluxyFlowRepository fluxyFlowRepository;
    private final FlowStepRepository flowStepRepository;
    private final FluxyStepRepository fluxyStepRepository;

    /** Devuelve todos los flows registrados. */
    public List<FluxyFlowDto> findAll() {
        return fluxyFlowRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    /** Busca un flow por su ID. */
    public Optional<FluxyFlowDto> findById(UUID id) {
        return fluxyFlowRepository.findById(id).map(this::toDto);
    }

    /** Busca un flow por su nombre. */
    public Optional<FluxyFlowDto> findByName(String name) {
        return fluxyFlowRepository.findByName(name).map(this::toDto);
    }

    /** Devuelve todos los flows de un tipo dado. */
    public List<FluxyFlowDto> findByType(String type) {
        return fluxyFlowRepository.findByType(type).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Crea un nuevo flow en la base de datos.
     */
    public FluxyFlowDto create(CreateFluxyFlowRequest request) {
        FluxyFlowEntity entity = new FluxyFlowEntity();
        entity.setName(request.name());
        entity.setType(request.type());
        entity.setDescription(request.description());
        return toDto(fluxyFlowRepository.save(entity));
    }

    /**
     * Agrega un step a un flow existente con el orden indicado.
     *
     * @throws IllegalArgumentException si el flow o el step no existen
     */
    public FlowStepDto addStep(UUID flowId, AddStepToFlowRequest request) {
        FluxyFlowEntity flow = fluxyFlowRepository.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("Flow no encontrado: " + flowId));
        FluxyStepEntity step = fluxyStepRepository.findById(request.stepId())
                .orElseThrow(() -> new IllegalArgumentException("Step no encontrado: " + request.stepId()));

        FlowStepEntity flowStep = new FlowStepEntity();
        flowStep.setFlow(flow);
        flowStep.setStep(step);
        flowStep.setStepOrder(request.order());
        flowStep.setStepStatus(StepStatus.PENDING);
        return toFlowStepDto(flowStepRepository.save(flowStep));
    }

    /**
     * Agrega un step (identificado por nombre) a un flow (identificado por nombre).
     *
     * @throws IllegalArgumentException si el flow o el step no existen
     */
    public FlowStepDto addStepByName(String flowName, AddStepByNameToFlowRequest request) {
        FluxyFlowEntity flow = fluxyFlowRepository.findByName(flowName)
                .orElseThrow(() -> new IllegalArgumentException("Flow no encontrado: " + flowName));
        FluxyStepEntity step = fluxyStepRepository.findByName(request.stepName())
                .orElseThrow(() -> new IllegalArgumentException("Step no encontrado: " + request.stepName()));

        FlowStepEntity flowStep = new FlowStepEntity();
        flowStep.setFlow(flow);
        flowStep.setStep(step);
        flowStep.setStepOrder(request.order());
        flowStep.setStepStatus(StepStatus.PENDING);
        return toFlowStepDto(flowStepRepository.save(flowStep));
    }

    /**
     * Elimina un step de un flow.
     * Si el step no pertenece al flow, la operación no hace nada.
     */
    public void removeStep(UUID flowId, UUID stepId) {
        FluxyFlowEntity flow = fluxyFlowRepository.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("Flow no encontrado: " + flowId));
        flowStepRepository.findByFlow(flow).stream()
                .filter(fs -> fs.getStep().getId().equals(stepId))
                .findFirst()
                .ifPresent(flowStepRepository::delete);
    }

    /**
     * Elimina un step (por nombre) de un flow (por nombre).
     */
    public void removeStepByName(String flowName, String stepName) {
        FluxyFlowEntity flow = fluxyFlowRepository.findByName(flowName)
                .orElseThrow(() -> new IllegalArgumentException("Flow no encontrado: " + flowName));
        flowStepRepository.findByFlow(flow).stream()
                .filter(fs -> fs.getStep().getName().equals(stepName))
                .findFirst()
                .ifPresent(flowStepRepository::delete);
    }

    /** Elimina un flow por su ID. */
    public void delete(UUID id) {
        if (!fluxyFlowRepository.existsById(id)) {
            throw new IllegalArgumentException("Flow no encontrado con ID: " + id);
        }
        fluxyFlowRepository.deleteById(id);
    }

    /** Elimina un flow por su nombre. */
    public void deleteByName(String name) {
        FluxyFlowEntity flow = fluxyFlowRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Flow no encontrado con nombre: " + name));
        fluxyFlowRepository.delete(flow);
    }

    // ── Mapeo ────────────────────────────────────────────────────────────────

    private FluxyFlowDto toDto(FluxyFlowEntity entity) {
        List<FlowStepDto> steps = entity.getSteps().stream()
                .map(this::toFlowStepDto)
                .toList();
        return new FluxyFlowDto(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getDescription(),
                steps
        );
    }

    private FlowStepDto toFlowStepDto(FlowStepEntity entity) {
        return new FlowStepDto(
                entity.getId(),
                entity.getStep().getId(),
                entity.getStep().getName(),
                entity.getStepOrder(),
                entity.getStepStatus() != null ? entity.getStepStatus().name() : null
        );
    }
}

