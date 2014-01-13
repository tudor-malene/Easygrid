package org.grails.plugin.easygrid

import org.springframework.dao.DataIntegrityViolationException
import org.grails.plugin.easygrid.grids.TestGridService

@Easygrid
class TestDomainController {
    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def grailsApplication
    static grids = {
        testGrid {
            dataSourceType 'gorm'
            domainClass TestDomain
            gridRenderer '/templates/easygrid/testGridRenderer'
            jqgrid {
                width 300
                height 150
            }
        }

        testGlobalFilterGrid {
            dataSourceType 'gorm'
            domainClass TestDomain
            globalFilterClosure {
                eq('testIntProperty', grailsApplication.domainClasses.size())
            }
        }

        visGrid {
            dataSourceType 'gorm'
            domainClass TestDomain
            gridImpl 'visualization'
        }

        test1 {
            dataSourceType 'gorm'
            domainClass TestDomain
            gridRenderer '/templates/easygrid/testGridRenderer'
            gridImplService TestGridService
        }

    }

    def index() {
        redirect(action: "list", params: params)
    }

    def list(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        [testDomainInstanceList: TestDomain.list(params), testDomainInstanceTotal: TestDomain.count()]
    }

    def create() {
        [testDomainInstance: new TestDomain(params)]
    }

    def save() {
        def testDomainInstance = new TestDomain(params)
        if (!testDomainInstance.save(flush: true)) {
            render(view: "create", model: [testDomainInstance: testDomainInstance])
            return
        }

        flash.message = message(code: 'default.created.message', args: [message(code: 'testDomain.label', default: 'TestDomain'), testDomainInstance.id])
        redirect(action: "show", id: testDomainInstance.id)
    }

    def show(Long id) {
        def testDomainInstance = TestDomain.get(id)
        if (!testDomainInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'testDomain.label', default: 'TestDomain'), id])
            redirect(action: "list")
            return
        }

        [testDomainInstance: testDomainInstance]
    }

    def edit(Long id) {
        def testDomainInstance = TestDomain.get(id)
        if (!testDomainInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'testDomain.label', default: 'TestDomain'), id])
            redirect(action: "list")
            return
        }

        [testDomainInstance: testDomainInstance]
    }

    def update(Long id, Long version) {
        def testDomainInstance = TestDomain.get(id)
        if (!testDomainInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'testDomain.label', default: 'TestDomain'), id])
            redirect(action: "list")
            return
        }

        if (version != null) {
            if (testDomainInstance.version > version) {
                testDomainInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                        [message(code: 'testDomain.label', default: 'TestDomain')] as Object[],
                        "Another user has updated this TestDomain while you were editing")
                render(view: "edit", model: [testDomainInstance: testDomainInstance])
                return
            }
        }

        testDomainInstance.properties = params

        if (!testDomainInstance.save(flush: true)) {
            render(view: "edit", model: [testDomainInstance: testDomainInstance])
            return
        }

        flash.message = message(code: 'default.updated.message', args: [message(code: 'testDomain.label', default: 'TestDomain'), testDomainInstance.id])
        redirect(action: "show", id: testDomainInstance.id)
    }

    def delete(Long id) {
        def testDomainInstance = TestDomain.get(id)
        if (!testDomainInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'testDomain.label', default: 'TestDomain'), id])
            redirect(action: "list")
            return
        }

        try {
            testDomainInstance.delete(flush: true)
            flash.message = message(code: 'default.deleted.message', args: [message(code: 'testDomain.label', default: 'TestDomain'), id])
            redirect(action: "list")
        }
        catch (DataIntegrityViolationException e) {
            flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'testDomain.label', default: 'TestDomain'), id])
            redirect(action: "show", id: id)
        }
    }
}
