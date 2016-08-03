function [U,ExtrVal] = femcodeJTJava(pts, tri, bnd, bndHt, ht, nBnd, bndType, bndFunc)
%set pts, tri, bnd, ht, bndType, bndFunc before calling
%function U=femcodeJTJava()
%   will calculate finite element analysis on passed mesh
%   pts : list of points in x,y
%   tri : list of poly triangles using idx's of points in pts list
%   bnd : list of idxs of pts on boundary
%   ht : heights of finite element pyramids
%   bndType : type of boundary TODO assume dirclet currently
%   bndFunc : function describing boundary conditions TODO
   
    % %BUILD K MATRIX AND F VECTOR : 
    % K matrix : Stiffness matrix - defines finite element function across mesh
    % F vector : model vector
    Npts=size(pts,1);                           % number of points
    K=sparse(Npts,Npts);                        % Start stiffness matrix as sparse matrix, since will be mostly 0
    F=zeros(Npts,1);                            % load vector F to hold integrals of phi's times load f(x,y)

    [K,F] = solvePoisFE(pts, tri, ht);    
    %bndType : boundary type
    %bndFunc : functional value at boundary for appropriate bnd type 
    %bndFunc = 0;
    %[Kb,Fb] = setBnds(pts, bnd, bndHt, K, F, ht, nBnd, bndFunc);
    [Kb,Fb] = setBnds(pts, bnd, bndHt, K, F, ht, nBnd, bndFunc);
    % % SOLVE POISSON EQ
    U=Kb\Fb;                                    % The FEM approximation is U1 phi1 + ... + UNphiN
    %U=K\F;                                    % The FEM approximation is U1 phi1 + ... + UNphiN
    ExtrVal = zeros(2,1);    
    ExtrVal(1) = min(U); 
    ExtrVal(2) = max(U);
    %Plot the FEM approximation U(x,y) with values U_1 to U_N at the nodes 
%     trisurf(tri,pts(:,1),pts(:,2),0*pts(:,1),U,'edgecolor','k','facecolor','interp');
%     view(2),axis([min(min(pts,[],1)) max(max(pts,[],1)) min(min(pts,[],2)) max(max(pts,[],2))]),axis equal,colorbar;
    
end
