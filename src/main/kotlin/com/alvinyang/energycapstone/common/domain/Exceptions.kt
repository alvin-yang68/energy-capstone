package com.alvinyang.energycapstone.common.domain

open class DomainException(message: String) : RuntimeException(message)

class ResourceNotFoundException(message: String) : DomainException(message)

class DuplicateEntityException(message: String) : DomainException(message)
