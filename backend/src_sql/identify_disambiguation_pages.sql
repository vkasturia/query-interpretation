CREATE TABLE disambiguation_pages AS (
    SELECT p.page_id as dp_id, p.page_title as dp_title, rd_title as dp_redirect
    from page as p
             left join categorylinks ON cl_from = page_id
             left join redirect on page_id = rd_from
    where (p.page_title like '%(disambiguation)' or cl_to = 'All_disambiguation_pages') and p.page_namespace = 0
    group by p.page_id, p.page_title
);