package com.example.intermediate.repository;

import com.example.intermediate.domain.Post;
import com.example.intermediate.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
  List<Post> findAllByOrderByModifiedAtDesc();



  List<Post> findAllByMember(Member member);
}
