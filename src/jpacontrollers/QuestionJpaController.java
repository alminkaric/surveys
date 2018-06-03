/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jpacontrollers;

import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import entities.Answer;
import entities.Question;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import jpacontrollers.exceptions.NonexistentEntityException;

/**
 *
 * @author almin
 */
public class QuestionJpaController implements Serializable {

    public QuestionJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Question question) {
        if (question.getAnswerList() == null) {
            question.setAnswerList(new ArrayList<Answer>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            List<Answer> attachedAnswerList = new ArrayList<Answer>();
            for (Answer answerListAnswerToAttach : question.getAnswerList()) {
                answerListAnswerToAttach = em.getReference(answerListAnswerToAttach.getClass(), answerListAnswerToAttach.getId());
                attachedAnswerList.add(answerListAnswerToAttach);
            }
            question.setAnswerList(attachedAnswerList);
            em.persist(question);
            for (Answer answerListAnswer : question.getAnswerList()) {
                answerListAnswer.getQuestionList().add(question);
                answerListAnswer = em.merge(answerListAnswer);
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Question question) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Question persistentQuestion = em.find(Question.class, question.getId());
            List<Answer> answerListOld = persistentQuestion.getAnswerList();
            List<Answer> answerListNew = question.getAnswerList();
            List<Answer> attachedAnswerListNew = new ArrayList<Answer>();
            for (Answer answerListNewAnswerToAttach : answerListNew) {
                answerListNewAnswerToAttach = em.getReference(answerListNewAnswerToAttach.getClass(), answerListNewAnswerToAttach.getId());
                attachedAnswerListNew.add(answerListNewAnswerToAttach);
            }
            answerListNew = attachedAnswerListNew;
            question.setAnswerList(answerListNew);
            question = em.merge(question);
            for (Answer answerListOldAnswer : answerListOld) {
                if (!answerListNew.contains(answerListOldAnswer)) {
                    answerListOldAnswer.getQuestionList().remove(question);
                    answerListOldAnswer = em.merge(answerListOldAnswer);
                }
            }
            for (Answer answerListNewAnswer : answerListNew) {
                if (!answerListOld.contains(answerListNewAnswer)) {
                    answerListNewAnswer.getQuestionList().add(question);
                    answerListNewAnswer = em.merge(answerListNewAnswer);
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = question.getId();
                if (findQuestion(id) == null) {
                    throw new NonexistentEntityException("The question with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Integer id) throws NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Question question;
            try {
                question = em.getReference(Question.class, id);
                question.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The question with id " + id + " no longer exists.", enfe);
            }
            List<Answer> answerList = question.getAnswerList();
            for (Answer answerListAnswer : answerList) {
                answerListAnswer.getQuestionList().remove(question);
                answerListAnswer = em.merge(answerListAnswer);
            }
            em.remove(question);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Question> findQuestionEntities() {
        return findQuestionEntities(true, -1, -1);
    }

    public List<Question> findQuestionEntities(int maxResults, int firstResult) {
        return findQuestionEntities(false, maxResults, firstResult);
    }

    private List<Question> findQuestionEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Question.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public Question findQuestion(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Question.class, id);
        } finally {
            em.close();
        }
    }

    public int getQuestionCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Question> rt = cq.from(Question.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
